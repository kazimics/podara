package app.podiumpodcasts.podium.desktop.player

import app.podiumpodcasts.podium.utils.Logger

private const val TAG = "WsolaTimeStretch"

class WsolaTimeStretch(
    private val sampleRate: Int = 44100,
    private val channels: Int = 2
) {
    private val frameSize = 2048

    fun process(input: ShortArray, speed: Float): ShortArray {
        if (speed == 1.0f) return input

        val output = mutableListOf<Short>()
        var pos = 0

        while (pos + frameSize <= input.size) {
            val frame = input.copyOfRange(pos, pos + frameSize)
            val stretched = stretchFrame(frame, speed)
            output.addAll(stretched.toList())
            pos += (frameSize * speed).toInt().coerceAtLeast(1)
        }

        while (pos < input.size) {
            output.add(input[pos])
            pos++
        }

        return output.toShortArray()
    }

    private fun stretchFrame(frame: ShortArray, speed: Float): ShortArray {
        val outputSize = (frame.size / speed).toInt()
        val output = ShortArray(outputSize)

        val analysisStep = (frameSize * speed).toInt()
        val synthesisStep = frameSize

        var analysisPos = 0
        var synthesisPos = 0

        while (synthesisPos + synthesisStep <= output.size && analysisPos + analysisStep + frameSize <= frame.size) {
            val bestOffset = findBestOverlap(frame, analysisPos, synthesisStep)

            for (i in 0 until synthesisStep) {
                val fadeIn = i.toFloat() / synthesisStep
                val fadeOut = 1.0f - fadeIn
                val srcIdx = analysisPos + bestOffset + i
                if (srcIdx < frame.size && synthesisPos + i < output.size) {
                    output[synthesisPos + i] = (frame[srcIdx] * fadeOut).toInt().toShort()
                }
            }

            analysisPos += analysisStep
            synthesisPos += synthesisStep
        }

        return output
    }

    private fun findBestOverlap(frame: ShortArray, analysisPos: Int, step: Int): Int {
        var bestOffset = 0
        var bestCorrelation = Long.MIN_VALUE

        val searchRange = step / 4
        val refStart = analysisPos
        val refEnd = (refStart + frameSize).coerceAtMost(frame.size)

        if (refEnd - refStart < frameSize) return 0

        for (offset in -searchRange..searchRange) {
            val testStart = analysisPos + offset
            val testEnd = (testStart + frameSize).coerceAtMost(frame.size)
            if (testStart < 0 || testEnd - testStart < frameSize) continue

            var correlation = 0L
            for (i in 0 until frameSize) {
                correlation += frame[refStart + i].toLong() * frame[testStart + i].toLong()
            }

            if (correlation > bestCorrelation) {
                bestCorrelation = correlation
                bestOffset = offset
            }
        }

        return bestOffset.coerceAtLeast(0)
    }
}
