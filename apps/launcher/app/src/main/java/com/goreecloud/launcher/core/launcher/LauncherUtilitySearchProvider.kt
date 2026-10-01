package com.goreecloud.launcher.core.launcher

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs

/**
 * Local-only utility result action. Clipboard access happens only after the user explicitly taps
 * the result; queries/results are never retained by this provider.
 */
data class LauncherCopyTextSearchAction(
    val text: String,
) : LauncherSearchAction

/**
 * Permissionless, offline utility provider for quick arithmetic and common unit conversions.
 *
 * Supported arithmetic: +, -, *, /, %, parentheses, unary +/-, ×/÷, and simple "% of" queries.
 * Supported conversion dimensions: length, mass, volume, time, and temperature.
 */
class LauncherUtilitySearchProvider : LauncherSearchProvider {
    override val id: String = PROVIDER_ID

    override fun search(rawQuery: String): List<LauncherSearchResult> {
        val query = rawQuery.trim()
        if (query.isBlank()) return emptyList()

        val evaluation =
            LauncherUtilityQueryParser.convert(query)
                ?: LauncherUtilityQueryParser.evaluatePercentage(query)
                ?: LauncherUtilityQueryParser.evaluateExpression(query)
                ?: return emptyList()

        return listOf(
            LauncherSearchResult(
                providerId = id,
                resultId = evaluation.kind + ":" + query.lowercase(Locale.ROOT),
                title = evaluation.resultText,
                subtitle = evaluation.subtitle,
                category = LauncherSearchCategory.UTILITY,
                score = RESULT_SCORE,
                action = LauncherCopyTextSearchAction(evaluation.resultText),
            ),
        )
    }

    companion object {
        const val PROVIDER_ID = "launcher.utility"
        private const val RESULT_SCORE = 600
    }
}

internal data class LauncherUtilityEvaluation(
    val kind: String,
    val resultText: String,
    val subtitle: String,
)

internal object LauncherUtilityQueryParser {
    private val conversionPattern = Regex(
        pattern = """^([+-]?(?:\d+(?:\.\d*)?|\.\d+))\s*([a-zA-Z°]+(?:\s+[a-zA-Z°]+)?)\s+(?:to|in)\s+([a-zA-Z°]+(?:\s+[a-zA-Z°]+)?)$""",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val percentagePattern = Regex(
        pattern = """^([+-]?(?:\d+(?:\.\d*)?|\.\d+))\s*%\s+of\s+([+-]?(?:\d+(?:\.\d*)?|\.\d+))$""",
        options = setOf(RegexOption.IGNORE_CASE),
    )
    private val allowedExpressionCharacters = Regex("""^[0-9+\-*/%().×÷\s]+$""")

    fun evaluateExpression(rawQuery: String): LauncherUtilityEvaluation? {
        val trimmed = rawQuery.trim()
        val normalized = trimmed
            .removePrefix("=")
            .trim()
            .replace('×', '*')
            .replace('÷', '/')
        if (
            normalized.isBlank() ||
            !allowedExpressionCharacters.matches(normalized) ||
            !containsBinaryOperator(normalized)
        ) {
            return null
        }

        val value = runCatching { ExpressionParser(normalized).parse() }.getOrNull()
            ?: return null
        if (!value.isFinite()) return null

        val formatted = formatNumber(value)
        return LauncherUtilityEvaluation(
            kind = "calculator",
            resultText = formatted,
            subtitle = "$trimmed · Local calculator · Tap to copy",
        )
    }

    fun evaluatePercentage(rawQuery: String): LauncherUtilityEvaluation? {
        val match = percentagePattern.matchEntire(rawQuery.trim()) ?: return null
        val percent = match.groupValues[1].toDoubleOrNull() ?: return null
        val value = match.groupValues[2].toDoubleOrNull() ?: return null
        if (!percent.isFinite() || !value.isFinite()) return null

        val result = value * percent / 100.0
        if (!result.isFinite()) return null
        return LauncherUtilityEvaluation(
            kind = "percentage",
            resultText = formatNumber(result),
            subtitle = "${formatNumber(percent)}% of ${formatNumber(value)} · Local calculator · Tap to copy",
        )
    }

    fun convert(rawQuery: String): LauncherUtilityEvaluation? {
        val match = conversionPattern.matchEntire(rawQuery.trim()) ?: return null
        val input = match.groupValues[1].toDoubleOrNull() ?: return null
        if (!input.isFinite()) return null

        val from = unitFor(match.groupValues[2]) ?: return null
        val to = unitFor(match.groupValues[3]) ?: return null
        if (from.dimension != to.dimension) return null

        val converted = if (from.dimension == Dimension.TEMPERATURE) {
            from.convertTemperatureTo(to, input)
        } else {
            input * from.toBaseFactor / to.toBaseFactor
        }
        if (!converted.isFinite()) return null

        val resultText =
            "${formatNumber(input)} ${from.symbol} = ${formatNumber(converted)} ${to.symbol}"
        return LauncherUtilityEvaluation(
            kind = "conversion",
            resultText = resultText,
            subtitle = "Local conversion · Tap to copy",
        )
    }

    private fun containsBinaryOperator(expression: String): Boolean {
        var previousNonSpace: Char? = null
        expression.forEach { char ->
            if (char.isWhitespace()) return@forEach
            if (char in charArrayOf('+', '-', '*', '/', '%')) {
                val unary = (char == '+' || char == '-') &&
                    (previousNonSpace == null || previousNonSpace in charArrayOf('(', '+', '-', '*', '/', '%'))
                if (!unary) return true
            }
            previousNonSpace = char
        }
        return false
    }

    private enum class Dimension {
        LENGTH,
        MASS,
        VOLUME,
        TIME,
        TEMPERATURE,
    }

    private data class UnitDefinition(
        val dimension: Dimension,
        val symbol: String,
        val aliases: Set<String>,
        val toBaseFactor: Double = 1.0,
        val temperatureKind: TemperatureKind? = null,
    ) {
        fun convertTemperatureTo(target: UnitDefinition, value: Double): Double {
            val sourceKind = checkNotNull(temperatureKind)
            val targetKind = checkNotNull(target.temperatureKind)
            val celsius = when (sourceKind) {
                TemperatureKind.CELSIUS -> value
                TemperatureKind.FAHRENHEIT -> (value - 32.0) * (5.0 / 9.0)
                TemperatureKind.KELVIN -> value - 273.15
            }
            return when (targetKind) {
                TemperatureKind.CELSIUS -> celsius
                TemperatureKind.FAHRENHEIT -> celsius * (9.0 / 5.0) + 32.0
                TemperatureKind.KELVIN -> celsius + 273.15
            }
        }
    }

    private enum class TemperatureKind {
        CELSIUS,
        FAHRENHEIT,
        KELVIN,
    }

    private val units = listOf(
        UnitDefinition(Dimension.LENGTH, "mm", setOf("mm", "millimeter", "millimeters"), 0.001),
        UnitDefinition(Dimension.LENGTH, "cm", setOf("cm", "centimeter", "centimeters"), 0.01),
        UnitDefinition(Dimension.LENGTH, "m", setOf("m", "meter", "meters", "metre", "metres"), 1.0),
        UnitDefinition(Dimension.LENGTH, "km", setOf("km", "kilometer", "kilometers", "kilometre", "kilometres"), 1000.0),
        UnitDefinition(Dimension.LENGTH, "in", setOf("in", "inch", "inches"), 0.0254),
        UnitDefinition(Dimension.LENGTH, "ft", setOf("ft", "foot", "feet"), 0.3048),
        UnitDefinition(Dimension.LENGTH, "yd", setOf("yd", "yard", "yards"), 0.9144),
        UnitDefinition(Dimension.LENGTH, "mi", setOf("mi", "mile", "miles"), 1609.344),

        UnitDefinition(Dimension.MASS, "g", setOf("g", "gram", "grams"), 0.001),
        UnitDefinition(Dimension.MASS, "kg", setOf("kg", "kilogram", "kilograms"), 1.0),
        UnitDefinition(Dimension.MASS, "oz", setOf("oz", "ounce", "ounces"), 0.028349523125),
        UnitDefinition(Dimension.MASS, "lb", setOf("lb", "lbs", "pound", "pounds"), 0.45359237),

        UnitDefinition(Dimension.VOLUME, "mL", setOf("ml", "milliliter", "milliliters", "millilitre", "millilitres"), 0.001),
        UnitDefinition(Dimension.VOLUME, "L", setOf("l", "liter", "liters", "litre", "litres"), 1.0),
        UnitDefinition(Dimension.VOLUME, "tsp", setOf("tsp", "teaspoon", "teaspoons"), 0.00492892159375),
        UnitDefinition(Dimension.VOLUME, "tbsp", setOf("tbsp", "tablespoon", "tablespoons"), 0.01478676478125),
        UnitDefinition(
            Dimension.VOLUME,
            "fl oz",
            setOf("floz", "fl oz", "fluidounce", "fluidounces", "fluid ounce", "fluid ounces"),
            0.0295735295625,
        ),
        UnitDefinition(Dimension.VOLUME, "cup", setOf("cup", "cups"), 0.2365882365),
        UnitDefinition(Dimension.VOLUME, "gal", setOf("gal", "gallon", "gallons"), 3.785411784),

        UnitDefinition(Dimension.TIME, "s", setOf("s", "sec", "second", "seconds"), 1.0),
        UnitDefinition(Dimension.TIME, "min", setOf("min", "minute", "minutes"), 60.0),
        UnitDefinition(Dimension.TIME, "h", setOf("h", "hr", "hour", "hours"), 3600.0),
        UnitDefinition(Dimension.TIME, "day", setOf("day", "days"), 86400.0),

        UnitDefinition(Dimension.TEMPERATURE, "°C", setOf("c", "°c", "celsius"), temperatureKind = TemperatureKind.CELSIUS),
        UnitDefinition(Dimension.TEMPERATURE, "°F", setOf("f", "°f", "fahrenheit"), temperatureKind = TemperatureKind.FAHRENHEIT),
        UnitDefinition(Dimension.TEMPERATURE, "K", setOf("k", "kelvin"), temperatureKind = TemperatureKind.KELVIN),
    )

    private val unitsByAlias = buildMap {
        units.forEach { definition ->
            definition.aliases.forEach { alias ->
                put(alias.lowercase(Locale.ROOT), definition)
            }
        }
    }

    private fun unitFor(raw: String): UnitDefinition? =
        unitsByAlias[raw.trim().lowercase(Locale.ROOT)]

    private fun formatNumber(value: Double): String {
        if (abs(value) < 1e-12) return "0"
        return BigDecimal.valueOf(value)
            .setScale(10, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    private class ExpressionParser(
        private val input: String,
    ) {
        private var index = 0

        fun parse(): Double {
            val value = parseExpression()
            skipWhitespace()
            require(index == input.length) { "Unexpected trailing input" }
            require(value.isFinite()) { "Result is not finite" }
            return value
        }

        private fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipWhitespace()
                value = when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                skipWhitespace()
                value = when {
                    consume('*') -> value * parseUnary()
                    consume('/') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Division by zero" }
                        value / divisor
                    }
                    consume('%') -> {
                        val divisor = parseUnary()
                        require(divisor != 0.0) { "Modulo by zero" }
                        value % divisor
                    }
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            return when {
                consume('+') -> parseUnary()
                consume('-') -> -parseUnary()
                else -> parsePrimary()
            }
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            if (consume('(')) {
                val value = parseExpression()
                skipWhitespace()
                require(consume(')')) { "Missing closing parenthesis" }
                return value
            }
            return parseNumber()
        }

        private fun parseNumber(): Double {
            skipWhitespace()
            val start = index
            var decimalSeen = false
            while (index < input.length) {
                val char = input[index]
                when {
                    char.isDigit() -> index += 1
                    char == '.' && !decimalSeen -> {
                        decimalSeen = true
                        index += 1
                    }
                    else -> break
                }
            }
            require(index > start && input.substring(start, index) != ".") { "Number expected" }
            return input.substring(start, index).toDouble()
        }

        private fun consume(expected: Char): Boolean {
            if (index >= input.length || input[index] != expected) return false
            index += 1
            return true
        }

        private fun skipWhitespace() {
            while (index < input.length && input[index].isWhitespace()) index += 1
        }
    }
}
