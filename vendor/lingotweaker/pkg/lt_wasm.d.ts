/* tslint:disable */
/* eslint-disable */

/**
 * An immutable engine loaded from a data pack.
 */
export class LtEngine {
    free(): void;
    [Symbol.dispose](): void;
    /**
     * Number of rules actively matched by the pattern engine.
     */
    active_rule_count(): number;
    /**
     * Check `text` and return the LT-shaped JSON of `CheckResult`.
     */
    check_json(text: string): string;
    /**
     * Check `text` and return only the matches as JSON: `{"matches":[…]}`.
     *
     * The demo (and any UI that only renders issues) never needs the input
     * text and per-sentence text spans that [`Self::check_json`] also
     * serializes; for long paragraphs this JSON round-trip is a measurable
     * share of the per-check latency.
     */
    check_matches_json(text: string): string;
    /**
     * Rules that failed to compile, as `[{rule, error}, …]` JSON.
     */
    compile_failures_json(): string;
    /**
     * The language code this engine was built for.
     */
    lang(): string;
    /**
     * Build an engine for `lang` (e.g. `en-US`, `gn-ES`) from `pack`.
     *
     * `options` is a JSON object; all fields are optional:
     * `variant` (`en-GB`) selects variant resources, `today` (ISO date or
     * timestamp) pins the date filters, and `picky`, `enabledRules`,
     * `disabledRules`, `enabledCategories`, `disabledCategories`,
     * `enabledOnly` configure rule selection.
     */
    constructor(lang: string, pack: Uint8Array, options?: string | null);
    /**
     * Like [`LtEngine::new`] but built from several packs: the first is the
     * base pack, later ones are sidecars (split packs: `en` base + optional
     * `en.models`/`en-GB`-style variant dictionaries; reads fall through to
     * the later packs). `packs` is a JS array of `Uint8Array`s.
     */
    static new_multi(lang: string, packs: Uint8Array[], options?: string | null): LtEngine;
    /**
     * The variant override, if any.
     */
    variant(): string | undefined;
}

export function start(): void;

export type InitInput = RequestInfo | URL | Response | BufferSource | WebAssembly.Module;

export interface InitOutput {
    readonly memory: WebAssembly.Memory;
    readonly __wbg_ltengine_free: (a: number, b: number) => void;
    readonly ltengine_active_rule_count: (a: number) => number;
    readonly ltengine_check_json: (a: number, b: number, c: number) => [number, number, number, number];
    readonly ltengine_check_matches_json: (a: number, b: number, c: number) => [number, number, number, number];
    readonly ltengine_compile_failures_json: (a: number) => [number, number, number, number];
    readonly ltengine_lang: (a: number) => [number, number];
    readonly ltengine_new: (a: number, b: number, c: number, d: number, e: number, f: number) => [number, number, number];
    readonly ltengine_new_multi: (a: number, b: number, c: number, d: number, e: number, f: number) => [number, number, number];
    readonly ltengine_variant: (a: number) => [number, number];
    readonly start: () => void;
    readonly __wbindgen_free: (a: number, b: number, c: number) => void;
    readonly __wbindgen_malloc: (a: number, b: number) => number;
    readonly __wbindgen_realloc: (a: number, b: number, c: number, d: number) => number;
    readonly __wbindgen_externrefs: WebAssembly.Table;
    readonly __externref_table_dealloc: (a: number) => void;
    readonly __externref_table_alloc: () => number;
    readonly __wbindgen_start: () => void;
}

export type SyncInitInput = BufferSource | WebAssembly.Module;

/**
 * Instantiates the given `module`, which can either be bytes or
 * a precompiled `WebAssembly.Module`.
 *
 * @param {{ module: SyncInitInput }} module - Passing `SyncInitInput` directly is deprecated.
 *
 * @returns {InitOutput}
 */
export function initSync(module: { module: SyncInitInput } | SyncInitInput): InitOutput;

/**
 * If `module_or_path` is {RequestInfo} or {URL}, makes a request and
 * for everything else, calls `WebAssembly.instantiate` directly.
 *
 * @param {{ module_or_path: InitInput | Promise<InitInput> }} module_or_path - Passing `InitInput` directly is deprecated.
 *
 * @returns {Promise<InitOutput>}
 */
export default function __wbg_init (module_or_path?: { module_or_path: InitInput | Promise<InitInput> } | InitInput | Promise<InitInput>): Promise<InitOutput>;
