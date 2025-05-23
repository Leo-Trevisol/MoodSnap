package com.br.leo.moodsnap.ui.utils

import android.os.SystemClock
import android.view.View

/**
 * Utilitário para gerenciar cliques em elementos da UI e evitar múltiplos cliques rápidos.
 */
object ClickUtils {
    
    // Tempo mínimo entre cliques (em milissegundos)
    private const val DEFAULT_DEBOUNCE_TIME = 800L
    
    // Mapa para armazenar o último tempo de clique para cada view
    private val lastClickTimestamps = mutableMapOf<Int, Long>()
    
    /**
     * Configura um listener de clique com proteção contra múltiplos cliques rápidos.
     * 
     * @param view A view que receberá o listener de clique
     * @param debounceTime Tempo mínimo entre cliques em milissegundos
     * @param action Ação a ser executada quando o clique for válido
     */
    fun setDebounceClickListener(
        view: View,
        debounceTime: Long = DEFAULT_DEBOUNCE_TIME,
        action: (View) -> Unit
    ) {
        view.setOnClickListener {
            if (isClickValid(view.id, debounceTime)) {
                action(it)
            }
        }
    }
    
    /**
     * Verifica se um clique é válido com base no tempo desde o último clique.
     * 
     * @param viewId ID da view que foi clicada
     * @param debounceTime Tempo mínimo entre cliques em milissegundos
     * @return true se o clique for válido, false caso contrário
     */
    fun isClickValid(viewId: Int, debounceTime: Long = DEFAULT_DEBOUNCE_TIME): Boolean {
        val currentTime = SystemClock.elapsedRealtime()
        val lastClickTime = lastClickTimestamps[viewId] ?: 0L
        
        return if (currentTime - lastClickTime > debounceTime) {
            lastClickTimestamps[viewId] = currentTime
            true
        } else {
            false
        }
    }
}
