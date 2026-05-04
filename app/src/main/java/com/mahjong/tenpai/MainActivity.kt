package com.mahjong.tenpai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme()
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TenpaiScreen()
                }
            }
        }
    }
}

// ── Algorithm (ported from Rust) ──────────────────────────────────────

private data class WinResult(val melds: List<String>, val pair: String)

private fun findWinning(hand: IntArray, expectedMelds: Int): WinResult? {
    // hand is indexed 0-9, only 1-9 used; must be 10 elements
    for (i in 1..9) {
        if (hand[i] >= 2) {
            val rem = hand.copyOf()
            rem[i] -= 2
            val melds = mutableListOf<String>()
            if (tryMelds(rem, melds, expectedMelds)) {
                return WinResult(melds, "$i$i")
            }
        }
    }
    return null
}

private fun tryMelds(tiles: IntArray, melds: MutableList<String>, expectedMelds: Int): Boolean {
    val first = (1..9).firstOrNull { tiles[it] > 0 }
        ?: return melds.size == expectedMelds

    // Try triplet AAA
    if (tiles[first] >= 3) {
        tiles[first] -= 3
        melds.add("$first$first$first")
        if (tryMelds(tiles, melds, expectedMelds)) {
            tiles[first] += 3
            return true
        }
        melds.removeAt(melds.lastIndex)
        tiles[first] += 3
    }

    // Try sequence ABC
    if (first <= 7 && tiles[first + 1] > 0 && tiles[first + 2] > 0) {
        tiles[first] -= 1
        tiles[first + 1] -= 1
        tiles[first + 2] -= 1
        melds.add("$first${first + 1}${first + 2}")
        if (tryMelds(tiles, melds, expectedMelds)) {
            tiles[first] += 1
            tiles[first + 1] += 1
            tiles[first + 2] += 1
            return true
        }
        melds.removeAt(melds.lastIndex)
        tiles[first] += 1
        tiles[first + 1] += 1
        tiles[first + 2] += 1
    }

    return false
}

/** Main analysis: given hand tiles (4/7/10/13), find all tenpai waits. */
private fun analyzeTenpai(input: String): List<String> {
    val hand = IntArray(10)
    for (ch in input) {
        val d = ch.digitToIntOrNull()
        if (d != null && d in 1..9) {
            hand[d]++
        }
    }

    val total = hand.slice(1..9).sum()
    if (total !in listOf(4, 7, 10, 13)) {
        return listOf("牌数错误：$total（需要4/7/10/13张）")
    }

    val expectedMelds = (total - 1) / 3
    val results = mutableListOf<String>()

    for (t in 1..9) {
        if (hand[t] >= 4) continue
        val tiles = hand.copyOf()
        tiles[t]++
        val win = findWinning(tiles, expectedMelds)
        if (win != null) {
            val remaining = 4 - hand[t]
            val meldStr = win.melds.joinToString(" ")
            results.add("$t -> $meldStr ${win.pair} ($remaining)")
        }
    }

    return if (results.isEmpty()) listOf("未听牌") else results
}

// ── Compose UI ───────────────────────────────────────────────────────

@Composable
fun TenpaiScreen() {
    var input by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<String>>(emptyList()) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🀄 麻将听牌计算器",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "输入手牌数字（1-9），支持 4/7/10/13 张",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = input,
            onValueChange = { newValue ->
                // Only allow digits 1-9
                input = newValue.filter { it in '1'..'9' }
            },
            label = { Text("手牌") },
            placeholder = { Text("如 1112345678999") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { results = analyzeTenpai(input) },
            enabled = input.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("计算听牌", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (results.isNotEmpty()) {
            val isTenpai = results.first() != "未听牌" && !results.first().startsWith("牌数错误")

            Text(
                text = if (isTenpai) "✅ 听牌结果" else "❌ 结果",
                fontSize = 18.sp,
                color = if (isTenpai) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(scrollState)
                ) {
                    for (line in results) {
                        Text(
                            text = line,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
