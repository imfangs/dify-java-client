#!/usr/bin/env bash
# Isolated release checks: all Git writes stay in temporary repositories, and
# Maven, fetch and push are replaced by local stubs. No credentials are needed.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RELEASE_SCRIPT="${SCRIPT_DIR}/release.sh"
RELEASE_REAL_GIT="$(command -v git)"
RELEASE_TEST_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/dify-release-test.XXXXXX")"
RELEASE_TEST_ROOT="$(cd "${RELEASE_TEST_ROOT}" && pwd -P)"
trap 'rm -rf "${RELEASE_TEST_ROOT}"' EXIT
export RELEASE_REAL_GIT RELEASE_TEST_ROOT
export GIT_CONFIG_NOSYSTEM=1 GIT_CONFIG_GLOBAL=/dev/null

mkdir "${RELEASE_TEST_ROOT}/bin"
cat > "${RELEASE_TEST_ROOT}/bin/git" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
case "${PWD}/" in "${RELEASE_TEST_ROOT}/"*) ;; *) exit 99;; esac
printf 'git %s\n' "$*" >> "${RELEASE_TEST_LOG}"
case "$1" in fetch|push) exit 0;; esac
exec "${RELEASE_REAL_GIT}" "$@"
STUB
cat > "${RELEASE_TEST_ROOT}/bin/mvn" <<'STUB'
#!/usr/bin/env bash
set -euo pipefail
case "${PWD}/" in "${RELEASE_TEST_ROOT}/"*) ;; *) exit 99;; esac
printf 'mvn %s\n' "$*" >> "${RELEASE_TEST_LOG}"
case " $* " in
  *' help:evaluate '*)
    if [[ "${RELEASE_TEST_EVALUATE_FAIL}" == 1 ]]; then exit 13; fi
    printf '%s' "${RELEASE_TEST_VERSION}"
    ;;
  *' clean verify '*)
    if [[ "${RELEASE_TEST_BUILD_FAIL}" == 1 ]]; then exit 17; fi
    ;;
  *' -Prelease -DwaitUntil=PUBLISHED verify central-publishing:publish '*) ;;
  *) echo "Unexpected Maven command" >&2; exit 98;;
esac
STUB
chmod +x "${RELEASE_TEST_ROOT}/bin/git" "${RELEASE_TEST_ROOT}/bin/mvn"
export PATH="${RELEASE_TEST_ROOT}/bin:${PATH}"

fail() { echo "FAIL: $*" >&2; cat "${RELEASE_TEST_OUTPUT}" >&2; exit 1; }

new_repo() {
  TEST_REPO="${RELEASE_TEST_ROOT}/$1"
  RELEASE_TEST_LOG="${RELEASE_TEST_ROOT}/$1.commands"
  RELEASE_TEST_OUTPUT="${RELEASE_TEST_ROOT}/$1.output"
  export RELEASE_TEST_LOG
  export RELEASE_TEST_VERSION=1.2.3 RELEASE_TEST_EVALUATE_FAIL=0 RELEASE_TEST_BUILD_FAIL=0
  mkdir "${TEST_REPO}"
  : > "${RELEASE_TEST_LOG}"
  : > "${RELEASE_TEST_OUTPUT}"
  "${RELEASE_REAL_GIT}" -C "${TEST_REPO}" init -q
  "${RELEASE_REAL_GIT}" -C "${TEST_REPO}" symbolic-ref HEAD refs/heads/main
  "${RELEASE_REAL_GIT}" -C "${TEST_REPO}" config user.name 'Release Test'
  "${RELEASE_REAL_GIT}" -C "${TEST_REPO}" config user.email 'release-test@example.invalid'
  printf '<project><version>1.2.3</version></project>\n' > "${TEST_REPO}/pom.xml"
  "${RELEASE_REAL_GIT}" -C "${TEST_REPO}" add pom.xml
  "${RELEASE_REAL_GIT}" -C "${TEST_REPO}" commit -qm 'Initial test project'
}

run_release() {
  local expected="$1" actual=0
  shift
  (cd "${TEST_REPO}" && bash "${RELEASE_SCRIPT}" "$@") > "${RELEASE_TEST_OUTPUT}" 2>&1 || actual=$?
  [[ "${actual}" == "${expected}" ]] || fail "expected exit ${expected}, got ${actual}"
}

assert_no_release() {
  [[ -z "$("${RELEASE_REAL_GIT}" -C "${TEST_REPO}" tag --list)" ]] || fail 'unexpected tag'
  if grep -Eq '^git push |^mvn .*central-publishing:publish' "${RELEASE_TEST_LOG}"; then
    fail 'unexpected push or publication'
  fi
}

new_repo failed-build
export RELEASE_TEST_BUILD_FAIL=1
run_release 17 -v 1.2.3 --skip-remote-check -y
assert_no_release
echo 'PASS: failed verification creates no tag and performs no external write'

new_repo dry-run
run_release 0 -v 1.2.3 --skip-remote-check --dry-run -y
assert_no_release
if grep -Eq '^mvn .*clean verify|^git tag -a' "${RELEASE_TEST_LOG}"; then
  fail 'dry-run executed a build or created a tag'
fi
echo 'PASS: dry-run only evaluates the version and previews release operations'

for dirty_kind in tracked untracked; do
  new_repo "dirty-${dirty_kind}"
  if [[ "${dirty_kind}" == tracked ]]; then
    printf '\n' >> "${TEST_REPO}/pom.xml"
  else
    printf 'untracked\n' > "${TEST_REPO}/notes.txt"
  fi
  run_release 1 -v 1.2.3 --skip-remote-check -y
  assert_no_release
  grep -Fq '包括未跟踪文件' "${RELEASE_TEST_OUTPUT}" || fail 'missing dirty-worktree error'
done
echo 'PASS: tracked changes and untracked files are rejected'

new_repo version-mismatch
export RELEASE_TEST_VERSION=9.9.9
run_release 1 -v 1.2.3 --skip-remote-check -y
assert_no_release
grep -Fq '与指定版本 1.2.3 不一致' "${RELEASE_TEST_OUTPUT}" || fail 'missing version-mismatch error'

new_repo version-failed
export RELEASE_TEST_EVALUATE_FAIL=1
run_release 1 -v 1.2.3 --skip-remote-check -y
assert_no_release
grep -Fq '无法读取 Maven project.version' "${RELEASE_TEST_OUTPUT}" || fail 'missing version-read error'

new_repo version-empty
export RELEASE_TEST_VERSION=''
run_release 1 -v 1.2.3 --skip-remote-check -y
assert_no_release
echo 'PASS: mismatched, unavailable and empty project versions stop the release'

new_repo missing-value
for option in -v --version -n --notes -f --notes-file -b --branch; do
  run_release 2 "${option}"
  grep -Fq "参数 ${option} 缺少值" "${RELEASE_TEST_OUTPUT}" || fail 'missing option-value error'
done
assert_no_release
echo 'PASS: options with missing arguments fail promptly'

new_repo release-order
run_release 0 -v 1.2.3 --skip-remote-check -y
"${RELEASE_REAL_GIT}" -C "${TEST_REPO}" show-ref --verify --quiet refs/tags/v1.2.3 || fail 'missing release tag'
awk '
  /^mvn .* clean verify$/ { build = NR }
  /^git tag -a / { tag = NR }
  /^git push / { push = NR }
  /^mvn .* -Prelease -DwaitUntil=PUBLISHED verify central-publishing:publish$/ { publish = NR }
  END { exit !(build && build < tag && tag < push && push < publish) }
' "${RELEASE_TEST_LOG}" || fail 'incorrect verify/tag/push/publish order'
echo 'PASS: verification precedes the tag, push and release-profile publication'

new_repo no-push
run_release 0 -v 1.2.3 --skip-remote-check --no-push -y
if grep -q '^git push ' "${RELEASE_TEST_LOG}"; then fail '--no-push pushed a tag'; fi
grep -Fq -- '-Prelease -DwaitUntil=PUBLISHED verify central-publishing:publish' "${RELEASE_TEST_LOG}" || fail '--no-push unexpectedly disabled Maven publication'

new_repo no-publish
run_release 0 -v 1.2.3 --skip-remote-check --no-publish -y
grep -q '^git push ' "${RELEASE_TEST_LOG}" || fail '--no-publish unexpectedly disabled tag push'
if grep -q '^mvn .*central-publishing:publish' "${RELEASE_TEST_LOG}"; then fail '--no-publish published'; fi
echo 'PASS: --no-push and --no-publish remain independent'

new_repo remote-divergence
"${RELEASE_REAL_GIT}" -C "${TEST_REPO}" update-ref refs/remotes/origin/main HEAD
"${RELEASE_REAL_GIT}" -C "${TEST_REPO}" commit -qm 'Local ahead' --allow-empty
run_release 1 -v 1.2.3 -y
grep -Fq 'ahead=1, behind=0' "${RELEASE_TEST_OUTPUT}" || fail 'incorrect ahead count'
assert_no_release
"${RELEASE_REAL_GIT}" -C "${TEST_REPO}" update-ref refs/remotes/origin/main HEAD
"${RELEASE_REAL_GIT}" -C "${TEST_REPO}" update-ref refs/heads/main HEAD~1
run_release 1 -v 1.2.3 -y
grep -Fq 'ahead=0, behind=1' "${RELEASE_TEST_OUTPUT}" || fail 'incorrect behind count'
assert_no_release
echo 'PASS: remote divergence reports ahead and behind correctly'

echo 'All isolated release checks passed.'
