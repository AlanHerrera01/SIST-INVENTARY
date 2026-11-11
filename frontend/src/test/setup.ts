import "@testing-library/jest-dom/vitest"
import { cleanup } from "@testing-library/react"
import { afterEach } from "vitest"

// jsdom does not implement AbortSignal.timeout; the component only needs a signal.
if (!("timeout" in AbortSignal)) {
  Object.defineProperty(AbortSignal, "timeout", {
    value: () => new AbortController().signal,
  })
}

afterEach(() => cleanup())
