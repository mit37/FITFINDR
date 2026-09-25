# STANDARDS.md — binding, non-negotiable

These rules govern everything built in this repository, including honesty about
what is actually built vs. not built. They apply to every contributor —
human or AI coding agent — and to every session. This file, `AGENTS.md`, and
`CLAUDE.md` must stay byte-for-byte identical; if you update one, update all
three.

1. This is a v2 rebuild. The README's first line under the title MUST be
   exactly:
   > **v2 (2026 rebuild).** The original prototype's code was not preserved; this is a clean re-implementation of the same design. Numbers in this README come from this codebase.
2. NEVER backdate commits (no `GIT_AUTHOR_DATE`/`GIT_COMMITTER_DATE`/`--date`
   tricks).
3. NEVER invent metrics. Every number in the README must come from a script
   in this repo, reproducible with one command. If not measurable in this
   cloud instance (no Android SDK/emulator/phone/NPU here — verify with
   `which sdkmanager`, check for `$ANDROID_HOME`), say "not measured" and
   explain why, in both `docs/PLAN.md` and the README's "Cloud-instance
   constraints"/limitations section.
4. Report negative results, don't hide them.
5. README needs a "What this does not do" section stating real limitations
   honestly.
6. README footer: "Built with AI coding agents (Claude Code) under my
   direction; design, specs, review and evaluation are mine."
7. No secrets, API keys, model weights, datasets ever committed. `.gitignore`
   must cover `.env*`, `*.key`, `*.pem`, `secrets/`, `*.gguf`,
   `*.safetensors`, `*.bin`, `*.onnx`, `*.tflite`, `*.task`, and typical
   Android build artifacts.
8. Add gitleaks to CI (`gitleaks/gitleaks-action@v2`).
9. Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`), small
   and frequent, one logical change each. Do NOT squash into one giant
   commit — commit after each coherent step (scaffold, then parser, then
   color extractor, then UI, then CI, then docs), push at the end (or
   periodically if possible).
10. Repo owner is github.com/mit37/fitfindr, MIT license, default branch
    `main` (work happens on feature branches — never touch `main` directly
    from an agent session).
11. Topics (for repo settings — these can't be set via git, note them for
    whoever configures the GitHub repo): android, kotlin, jetpack-compose,
    on-device-ai, vision-language-model, mediapipe, litert, fashion.

## Milestone honesty

At any point in the project, this repo must clearly state (in `docs/PLAN.md`
and the README) which milestones are complete, which are in progress, and
which are not started. Never claim a milestone is "done" if any of its
acceptance criteria are unverified or stubbed.
