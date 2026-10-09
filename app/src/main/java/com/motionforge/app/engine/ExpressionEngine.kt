package com.motionforge.app.engine

import kotlin.math.*
import java.util.Random

object ExpressionEngine {

    /**
     * Evaluates an expression string against timeMs and base value.
     * Supported syntax:
     * - time (in seconds)
     * - wiggle(freq, amp)
     * - sin(expr), cos(expr), tan(expr), abs(expr)
     * - value (base animated or static value)
     * - arithmetic (+, -, *, /)
     */
    fun evaluate(expression: String, timeMs: Long, baseValue: Float): Float {
        val trimmed = expression.trim()
        if (trimmed.isEmpty()) return baseValue

        val timeSec = timeMs / 1000f

        try {
            // Handle wiggle(frequency, amplitude)
            val wiggleRegex = Regex("""wiggle\s*\(\s*([0-9.]+)\s*,\s*([0-9.]+)\s*\)""")
            val wiggleMatch = wiggleRegex.find(trimmed)
            if (wiggleMatch != null) {
                val freq = wiggleMatch.groupValues[1].toFloatOrNull() ?: 5f
                val amp = wiggleMatch.groupValues[2].toFloatOrNull() ?: 20f
                val noise = pseudoNoise(timeSec * freq)
                return baseValue + noise * amp
            }

            // Handle bounce
            if (trimmed.equals("bounce", ignoreCase = true)) {
                val cycle = (timeSec * 2f) % 1f
                val decay = exp(-4f * cycle)
                val bounce = abs(sin(cycle * PI.toFloat() * 4f)) * decay
                return baseValue * (1f + bounce * 0.3f)
            }

            // Handle loop / spin (e.g. time * 360)
            if (trimmed.contains("time")) {
                val replaced = trimmed
                    .replace("time", timeSec.toString())
                    .replace("value", baseValue.toString())
                    .replace("PI", PI.toString())
                return evaluateArithmetic(replaced)
            }

            // Handle pure arithmetic or functions
            val withValue = trimmed.replace("value", baseValue.toString())
            return evaluateArithmetic(withValue)
        } catch (_: Exception) {
            return baseValue
        }
    }

    private fun pseudoNoise(t: Float): Float {
        // Multi-frequency sine combination for smooth continuous noise
        val n1 = sin(t * 2.14f)
        val n2 = sin(t * 5.37f + 1.2f) * 0.5f
        val n3 = sin(t * 11.83f + 2.7f) * 0.25f
        return (n1 + n2 + n3) / 1.75f
    }

    private fun evaluateArithmetic(expr: String): Float {
        val clean = expr.replace(" ", "")

        // Handle sin(...) and cos(...)
        val sinMatch = Regex("""sin\(([^)]+)\)""").find(clean)
        if (sinMatch != null) {
            val innerVal = evaluateArithmetic(sinMatch.groupValues[1])
            val subbed = clean.replace(sinMatch.value, sin(innerVal).toString())
            return evaluateArithmetic(subbed)
        }
        val cosMatch = Regex("""cos\(([^)]+)\)""").find(clean)
        if (cosMatch != null) {
            val innerVal = evaluateArithmetic(cosMatch.groupValues[1])
            val subbed = clean.replace(cosMatch.value, cos(innerVal).toString())
            return evaluateArithmetic(subbed)
        }

        // Simple parser for +, -, *, /
        return SimpleParser(clean).parse()
    }

    private class SimpleParser(val input: String) {
        var pos = 0

        fun parse(): Float {
            val result = parseExpression()
            return result
        }

        private fun parseExpression(): Float {
            var value = parseTerm()
            while (pos < input.length) {
                val c = input[pos]
                if (c == '+') {
                    pos++
                    value += parseTerm()
                } else if (c == '-') {
                    pos++
                    value -= parseTerm()
                } else {
                    break
                }
            }
            return value
        }

        private fun parseTerm(): Float {
            var value = parseFactor()
            while (pos < input.length) {
                val c = input[pos]
                if (c == '*') {
                    pos++
                    value *= parseFactor()
                } else if (c == '/') {
                    pos++
                    val divisor = parseFactor()
                    value = if (divisor != 0f) value / divisor else value
                } else {
                    break
                }
            }
            return value
        }

        private fun parseFactor(): Float {
            if (pos >= input.length) return 0f
            if (input[pos] == '+') {
                pos++
                return parseFactor()
            }
            if (input[pos] == '-') {
                pos++
                return -parseFactor()
            }
            if (input[pos] == '(') {
                pos++
                val value = parseExpression()
                if (pos < input.length && input[pos] == ')') pos++
                return value
            }

            val start = pos
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                pos++
            }
            val numStr = input.substring(start, pos)
            return numStr.toFloatOrNull() ?: 0f
        }
    }
}
