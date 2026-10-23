package com.findly.app.data.remote

import com.findly.core.ai.AiIntent
import com.findly.core.ai.AiRefinementEngine
import com.findly.core.ai.RefinementRequest
import com.findly.core.ai.RefinementResult
import com.findly.core.ai.Verdict
import com.findly.core.model.Product

/**
 * AI refinement through the backend (LLM / vision re-ranking). Falls back to [fallback] for
 * anything the backend call cannot handle, so the user never sees invented judgements.
 */
class RemoteRefinementEngine(
    private val api: FindlyApi,
    private val fallback: AiRefinementEngine,
) : AiRefinementEngine {
    override val name = "Findly AI (backend)"

    override fun supports(intent: AiIntent) = true

    override suspend fun refine(products: List<Product>, request: RefinementRequest): RefinementResult {
        val response = runCatching {
            api.refine(RefineRequestDto(request.intents.map { it.name }, products.map { it.id }, request.freeText))
        }.getOrElse { return fallback.refine(products, request) }

        val byId = products.associateBy { it.id }
        val ordered = response.orderedIds.mapNotNull { byId[it] }
        // Never drop products the backend did not mention.
        val rest = products.filter { it.id !in response.orderedIds }
        return RefinementResult(
            products = ordered + rest,
            verdicts = response.verdicts.mapValues { (_, m) ->
                m.mapNotNull { (k, v) ->
                    val intent = AiIntent.entries.firstOrNull { it.name == k } ?: return@mapNotNull null
                    intent to (Verdict.entries.firstOrNull { it.name == v } ?: Verdict.UNKNOWN)
                }.toMap()
            },
            unsupported = response.unsupported.mapNotNull { u -> AiIntent.entries.firstOrNull { it.name == u } }.toSet(),
            engineName = name,
            isModelBased = true,
        )
    }
}
