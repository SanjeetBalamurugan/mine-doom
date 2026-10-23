---

## Project Maintainer

1. Sanjeet Balamurugan (Netherairtune) — Creator, C99 Core Port, JNI Bridge, & Minecraft 26.3 Integration. (Community contributors whose pull requests are merged will be listed here.)*

---

## 1. AI-Assisted Code Policy (Strictly Human-Reviewed)

Contributors **may** use AI tools (LLMs, autocomplete, refactoring assistants) to assist with drafting code, comments, or tests, **only under these mandatory rules**:

1. **100% Human Review & Comprehension:** You must personally read, test, and understand every single line of code in your PR before submitting. Opening a pull request with unverified AI output is not permitted. If asked during code review why a pointer cast, JNI buffer call, or Blaze3D state transition is written a certain way, *"the AI generated it"* will result in immediate PR closure.

2. **Mandatory Disclosure:** If AI tools assisted with your PR, include a brief one-line disclosure in the PR description noting which section was AI-assisted and how you verified it in-game.

3. **Zero Hallucinated APIs:** AI models frequently hallucinate outdated Minecraft Yarn mappings, raw `GL11` calls, or `GLFW` key constants that were removed in **Minecraft 26.1–26.3** . PRs containing hallucinated imports or dead boilerplate will be closed immediately.

---

## 2. Strict PR & Code Size Limits

Large diffs across C and Java make it nearly impossible to audit 64-bit pointer safety and JNI memory lifecycles. Every pull request must obey these limits:

* **Target Size:** focused on a single bug fix or self-contained improvement.

* **Zero Drive-By Formatting:** Never run auto-formatters (`clang-format`, IDE reformat) across untouched 1997 C files in `DOOM/linuxdoom-1.10/`. Keep diffs surgical so `git blame` against the 1997 source release stays clean.

---

## 3. Architectural & Engineering Rules

1. **Pre-PR Verification Checklist:**

   * Run `./scripts/build-all.sh` and `./gradlew build` locally with zero errors.

   * Test in a clean Minecraft `26.3` profile at both 60 FPS and uncapped (144+ FPS).

2. **License & Assets:**

   * All contributions are licensed under **`GPL-2.0-only`**.

   * Never commit `.wad` files, copyrighted soundfonts, or personal local paths.
