# Project instructions

Read HANDOFF.md, docs/IMPLEMENTATION_PLAN.md and docs/RESEARCH.md before implementation. The product interview and architecture decisions are complete; implementation is approved. Keep a progress record distinguishing implemented, automated-tested and physically-tested behavior.

Build native Android in Kotlin/Compose. Preserve the isolated core, offline behavior, explicit prayer tracking semantics, two reused calculation engines, GPL licensing and four-language accessibility requirements. Do not add Rust, a backend, analytics, generative AI, billing or unnecessary frameworks.

The visual identity must be original: contemporary Islamic geometry and a geometric next-prayer marker, not a generic card dashboard or Just Pray clone. Inspect actual screenshots at normal and large text sizes in both directions. Typography, spacing, contrast and motion are deliberate design work.

Development runs on Dukhan/cloud CI; real-device QA is local on Salman's S23 Ultra. Do not claim alarm/Doze/OEM behavior has passed without actual testing. Do not upload private user data or sign with production credentials during ordinary builds.

Use small reviewable milestones, meaningful correctness tests and pinned dependencies. Keep onboarding/product screens free of implementation details unless those details help users make a decision. No copyrighted or noncommercial-only audio/assets without an appropriate free redistribution license.
