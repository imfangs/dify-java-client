#!/usr/bin/env bash
set -euo pipefail

BLUE="\033[1;34m"
YELLOW="\033[1;33m"
RED="\033[1;31m"
NC="\033[0m"

log()   { echo -e "[${BLUE}INFO${NC}] $*"; }
warn()  { echo -e "[${YELLOW}WARN${NC}] $*"; }
error() { echo -e "[${RED}ERROR${NC}] $*" >&2; }

usage() {
  cat <<'EOF'
release.sh - 自动打 Tag 并发布到 Maven Central（Central Publishing 插件）

用法：
  bash scripts/release.sh -v <version> [-n <notes>] [-f <notes_file>] [选项]

必选参数：
  -v, --version <version>        版本号，例如 1.2.1

可选参数（二选一，若都省略则使用默认说明）：
  -n, --notes "<text>"           发行说明（多行请用 \n 分隔）
  -f, --notes-file <file>        从文件读取发行说明

其他选项：
  -b, --branch <name>            目标分支（默认：main）
      --no-push                  不推送 tag；仍会发布 Maven，除非同时指定 --no-publish
      --no-publish               不执行 Maven 发布
      --allow-dirty              允许工作区不干净
      --skip-remote-check        跳过与 origin/<branch> 的差异检查
      --dry-run                  仅打印将要执行的操作，不落地
  -y, --yes                      跳过交互确认
  -h, --help                     显示本帮助

前置要求：
  - gpg 已配置（maven-gpg-plugin 可用）
  - ~/.m2/settings.xml 配置了 serverId=central 的 Usertoken
  - 可访问 https://central.sonatype.com
EOF
}

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    error "缺少命令：$1。$2"
    exit 1
  fi
}

require_option_value() {
  if [[ $# -lt 2 ]]; then
    error "参数 $1 缺少值。"
    exit 2
  fi
}

# 默认参数
BRANCH="main"
DO_PUSH=1
DO_PUBLISH=1
ALLOW_DIRTY=0
SKIP_REMOTE_CHECK=0
DRY_RUN=0
YES=0
VERSION=""
NOTES=""
NOTES_FILE=""

# 解析参数
while [[ $# -gt 0 ]]; do
  case "$1" in
    -v|--version) require_option_value "$@"; VERSION="$2"; shift 2;;
    -n|--notes) require_option_value "$@"; NOTES="$2"; shift 2;;
    -f|--notes-file) require_option_value "$@"; NOTES_FILE="$2"; shift 2;;
    -b|--branch) require_option_value "$@"; BRANCH="$2"; shift 2;;
    --no-push) DO_PUSH=0; shift;;
    --no-publish) DO_PUBLISH=0; shift;;
    --allow-dirty) ALLOW_DIRTY=1; shift;;
    --skip-remote-check) SKIP_REMOTE_CHECK=1; shift;;
    --dry-run) DRY_RUN=1; shift;;
    -y|--yes) YES=1; shift;;
    -h|--help) usage; exit 0;;
    *) error "未知参数：$1"; usage; exit 2;;
  esac
done

if [[ -z "${VERSION}" ]]; then
  error "必须指定版本号，例如：-v 1.2.1"
  usage
  exit 2
fi

require_cmd git "请安装 Git。"
require_cmd mvn "请安装 Maven。"

# 进入仓库根目录
REPO_ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
if [[ -z "${REPO_ROOT}" ]]; then
  error "当前目录不在 Git 仓库中。"
  exit 1
fi
cd "${REPO_ROOT}"

log "仓库根目录：${REPO_ROOT}"

# 检查分支
CURRENT_BRANCH="$(git rev-parse --abbrev-ref HEAD)"
if [[ "${CURRENT_BRANCH}" != "${BRANCH}" ]]; then
  error "当前分支为 ${CURRENT_BRANCH}，预期 ${BRANCH}。使用 -b 切换目标分支或在正确分支运行。"
  exit 1
fi

# 工作区干净性
if [[ "${ALLOW_DIRTY}" -eq 0 ]]; then
  WORKTREE_STATUS="$(git status --porcelain --untracked-files=normal)"
  if [[ -n "${WORKTREE_STATUS}" ]]; then
    error "工作区存在未提交变更（包括未跟踪文件）。提交或清理后重试，或使用 --allow-dirty 跳过。"
    exit 1
  fi
else
  warn "已启用 --allow-dirty，跳过工作区干净性检查。"
fi

# 远程一致性
if [[ "${SKIP_REMOTE_CHECK}" -eq 0 ]]; then
  log "同步远程分支与标签（git fetch origin --prune --tags）"
  if [[ "${DRY_RUN}" -eq 1 ]]; then
    echo "DRY-RUN: git fetch origin --prune --tags"
  else
    git fetch origin --prune --tags
  fi
  DIFF="$(git rev-list --left-right --count "origin/${BRANCH}...${BRANCH}")"
  read -r BEHIND AHEAD <<< "${DIFF}"
  if [[ "${AHEAD}" != "0" || "${BEHIND}" != "0" ]]; then
    error "本地与 origin/${BRANCH} 不一致（ahead=${AHEAD}, behind=${BEHIND}）。请先同步。"
    exit 1
  fi
else
  warn "已启用 --skip-remote-check，跳过与远程差异检查。"
fi

# 检查 tag 是否已存在
TAG="v${VERSION}"
if git show-ref --tags "refs/tags/${TAG}" >/dev/null 2>&1; then
  error "Tag ${TAG} 已存在。"
  exit 1
fi

# 读取项目版本以校验
if ! PROJECT_VERSION="$(mvn -q -Dstyle.color=never help:evaluate -Dexpression=project.version -DforceStdout)" \
    || [[ -z "${PROJECT_VERSION}" ]]; then
  error "无法读取 Maven project.version，停止发布。"
  exit 1
fi
if [[ "${PROJECT_VERSION}" != "${VERSION}" ]]; then
  error "pom.xml 中的 project.version=${PROJECT_VERSION} 与指定版本 ${VERSION} 不一致。"
  exit 1
fi

# 发行说明
if [[ -n "${NOTES_FILE}" ]]; then
  if [[ ! -f "${NOTES_FILE}" ]]; then
    error "发行说明文件不存在：${NOTES_FILE}"
    exit 1
  fi
  NOTES="$(cat "${NOTES_FILE}")"
fi
if [[ -z "${NOTES}" ]]; then
  NOTES="Release ${TAG}"
fi

echo
log "即将发布：${TAG}"
echo "Tag message:"
echo "----------------"
echo -e "${NOTES}"
echo "----------------"

if [[ "${YES}" -ne 1 ]]; then
  read -r -p "确认继续？(y/N) " ans
  if [[ "${ans}" != "y" && "${ans}" != "Y" ]]; then
    error "已取消。"
    exit 1
  fi
fi

# 在创建或推送 tag 前验证构建与离线测试。
log "验证构建与测试：mvn -B -ntp clean verify"
if [[ "${DRY_RUN}" -eq 1 ]]; then
  echo "DRY-RUN: mvn -B -ntp clean verify"
else
  mvn -B -ntp clean verify
fi

# 创建 tag
log "创建注释 tag：${TAG}"
if [[ "${DRY_RUN}" -eq 1 ]]; then
  echo -e "DRY-RUN: git tag -a ${TAG} -m \$'${TAG}\n\n${NOTES//\'/\'\"\'\"\'}'"
else
  git tag -a "${TAG}" -m $''"${TAG}"$'\n\n'"${NOTES}"''
fi

# 推送 tag
if [[ "${DO_PUSH}" -eq 1 ]]; then
  log "推送 tag 到 origin：${TAG}"
  if [[ "${DRY_RUN}" -eq 1 ]]; then
    echo "DRY-RUN: git push origin ${TAG}"
  else
    git push origin "${TAG}"
  fi
else
  warn "已禁用 push（--no-push）。"
fi

# 发布到 Maven Central
if [[ "${DO_PUBLISH}" -eq 1 ]]; then
  log "执行 Maven 发布：-Prelease verify central-publishing:publish"
  if [[ "${DRY_RUN}" -eq 1 ]]; then
    echo "DRY-RUN: mvn -B -ntp -Prelease verify central-publishing:publish"
  else
    mvn -B -ntp -Prelease verify central-publishing:publish
  fi
else
  warn "已禁用发布（--no-publish）。"
fi

log "完成：${TAG}"

