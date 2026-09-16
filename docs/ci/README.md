# CI workflow (manual step)

`build.yml` here is the GitHub Actions workflow for building FlexiCat.
Opus's GitHub App has no `workflows` permission, so it cannot push files into
`.github/workflows/` (GitHub rejects the push). Move it yourself once:

1. On GitHub open **Add file → Create new file**, path `.github/workflows/build.yml`.
2. Paste the contents of `docs/ci/build.yml` and commit to `main`.

After that the workflow runs on every push/PR. This copy can then be deleted.
