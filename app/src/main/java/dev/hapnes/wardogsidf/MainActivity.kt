package dev.hapnes.wardogsidf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WardogsIdfTheme {
                Surface(color = Console.Ink, modifier = Modifier.fillMaxSize()) {
                    IdfCalculatorScreen()
                }
            }
        }
    }
}

/** Only digits, an optional leading minus and one decimal separator. */
private val COORDINATE_INPUT = Regex("^-?\\d*([.,]\\d*)?$")

private fun Double.metersLabel(): String =
    String.format(Locale.US, "%,d", this.roundToInt())

/** Matches JavaScript's `Math.round` so displayed offsets equal the web tool's. */
private fun jsRoundToInt(value: Double): Int = floor(value + 0.5).toInt()

@Composable
fun IdfCalculatorScreen() {
    var weapon by rememberSaveable { mutableStateOf(Weapon.L81) }
    var firingX by rememberSaveable { mutableStateOf("") }
    var firingY by rememberSaveable { mutableStateOf("") }
    var targetX by rememberSaveable { mutableStateOf("") }
    var targetY by rememberSaveable { mutableStateOf("") }

    val solution = remember(weapon, firingX, firingY, targetX, targetY) {
        val fx = parseCoordinate(firingX)
        val fy = parseCoordinate(firingY)
        val tx = parseCoordinate(targetX)
        val ty = parseCoordinate(targetY)
        if (fx == null || fy == null || tx == null || ty == null) {
            null
        } else {
            solve(GridPoint(fx, fy), GridPoint(tx, ty), weapon)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header()
        ConsoleCard(weapon = weapon, onWeaponChange = { weapon = it }) {
            Fieldset(step = "01", title = "FIRING POSITION") {
                CoordinateRow(
                    x = firingX,
                    y = firingY,
                    onXChange = { firingX = it },
                    onYChange = { firingY = it },
                    labelPrefix = "FIRING",
                )
            }
            Fieldset(step = "02", title = "TARGET POSITION") {
                CoordinateRow(
                    x = targetX,
                    y = targetY,
                    onXChange = { targetX = it },
                    onYChange = { targetY = it },
                    labelPrefix = "TARGET",
                    lastFieldIsDone = true,
                )
            }
            ClearButton {
                firingX = ""
                firingY = ""
                targetX = ""
                targetY = ""
            }
            SolutionPanel(weapon = weapon, solution = solution)
        }
        Disclaimer()
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Kicker("WARDOGS INDIRECT FIRE")
        Text(
            text = "IDF CALCULATOR",
            color = Console.Bright,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Text(
            text = "Enter the firing position and the target map coordinates. " +
                "Each full grid square counts as 100 meters.",
            color = Console.Dim,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun ConsoleCard(
    weapon: Weapon,
    onWeaponChange: (Weapon) -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Console.Edge, RoundedCornerShape(6.dp))
            .background(Console.Panel, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Kicker("FIRE DIRECTION CENTER // 100 M GRID")
            Text(
                text = "LIVE",
                color = Console.Amber,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
            )
        }
        WeaponSelector(weapon = weapon, onWeaponChange = onWeaponChange)
        content()
    }
}

@Composable
private fun WeaponSelector(weapon: Weapon, onWeaponChange: (Weapon) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Kicker("WEAPON SYSTEM")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Weapon.entries.forEach { option ->
                val selected = option == weapon
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = if (selected) Console.Amber else Console.Edge,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .background(
                            color = if (selected) Console.Amber.copy(alpha = 0.14f) else Color.Transparent,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable { onWeaponChange(option) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.displayName,
                        color = if (selected) Console.Amber else Console.Dim,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Text(
            text = "Effective range: ${weapon.minRange.metersLabel()}–${weapon.maxRange.metersLabel()} m",
            color = Console.Dim,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun Fieldset(step: String, title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .background(Console.Edge, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(step, color = Console.Amber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Kicker(title)
        }
        content()
    }
}

@Composable
private fun CoordinateRow(
    x: String,
    y: String,
    onXChange: (String) -> Unit,
    onYChange: (String) -> Unit,
    labelPrefix: String,
    lastFieldIsDone: Boolean = false,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        CoordinateField(
            value = x,
            onValueChange = onXChange,
            label = "X",
            contentDescription = "$labelPrefix X coordinate",
            imeAction = ImeAction.Next,
            modifier = Modifier.weight(1f),
        )
        CoordinateField(
            value = y,
            onValueChange = onYChange,
            label = "Y",
            contentDescription = "$labelPrefix Y coordinate",
            imeAction = if (lastFieldIsDone) ImeAction.Done else ImeAction.Next,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun CoordinateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    contentDescription: String,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (COORDINATE_INPUT.matches(it)) onValueChange(it) },
        modifier = modifier,
        singleLine = true,
        label = { Text(label, fontSize = 11.sp) },
        placeholder = { Text(contentDescription.uppercase(Locale.US), fontSize = 11.sp) },
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction,
        ),
        shape = RoundedCornerShape(4.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Console.Amber,
            unfocusedBorderColor = Console.Edge,
            focusedTextColor = Console.Bright,
            unfocusedTextColor = Console.Bright,
            cursorColor = Console.Amber,
            focusedLabelColor = Console.Amber,
            unfocusedLabelColor = Console.Dim,
            focusedPlaceholderColor = Console.Dim,
            unfocusedPlaceholderColor = Console.Dim,
            focusedContainerColor = Console.PanelDeep,
            unfocusedContainerColor = Console.PanelDeep,
        ),
    )
}

@Composable
private fun ClearButton(onClear: () -> Unit) {
    TextButton(
        onClick = onClear,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Console.Edge, RoundedCornerShape(4.dp)),
        shape = RoundedCornerShape(4.dp),
    ) {
        Text(
            text = "CLEAR COORDINATES",
            color = Console.Dim,
            fontSize = 12.sp,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun SolutionPanel(weapon: Weapon, solution: FiringSolution?) {
    val status = when {
        solution == null -> "WAITING FOR COORDINATES"
        solution.isInRange -> "READY"
        else -> "OUT OF RANGE"
    }
    val statusColor = when {
        solution == null -> Console.Dim
        solution.isInRange -> Console.Signal
        else -> Console.Alarm
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Console.Edge, RoundedCornerShape(6.dp))
            .background(Console.PanelDeep, RoundedCornerShape(6.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Kicker("${weapon.shortName} FIRING SOLUTION")
            Text(
                text = status,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }

        if (solution == null) {
            Text(
                text = "ENTER ALL FOUR COORDINATES",
                color = Console.Bright,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "The solution updates automatically.",
                color = Console.Dim,
                fontSize = 12.sp,
            )
            return@Column
        }

        if (!solution.isInRange) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Console.Alarm.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .background(Console.Alarm.copy(alpha = 0.10f), RoundedCornerShape(4.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = solution.error,
                    color = Console.Alarm,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 17.sp,
                )
                Text(
                    text = "${weapon.displayName} effective range: " +
                        "${weapon.minRange.metersLabel()}–${weapon.maxRange.metersLabel()} m",
                    color = Console.Dim,
                    fontSize = 11.sp,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultCard(
                label = "RANGE",
                value = solution.rangeMeters.metersLabel(),
                unit = "M",
                modifier = Modifier.weight(1f),
            )
            ResultCard(
                label = "BEARING",
                value = jsRoundToInt(solution.bearingDegrees).toString().padStart(3, '0'),
                unit = "°",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResultCard(
                label = "DIRECTION",
                value = solution.direction,
                unit = null,
                modifier = Modifier.weight(1f),
            )
            if (weapon.hasElevation) {
                ResultCard(
                    label = "EST. ELEVATION",
                    value = if (solution.isInRange) solution.elevationMils.toString() else "—",
                    unit = if (solution.isInRange) "MIL" else null,
                    modifier = Modifier.weight(1f),
                )
            } else {
                ResultCard(
                    label = "WEAPON",
                    value = weapon.shortName,
                    unit = null,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        OffsetRow(east = solution.east, north = solution.north)

        Text(
            text = when {
                !solution.isInRange ->
                    "No valid firing solution for the selected weapon at this distance."

                weapon.hasElevation ->
                    "Elevation is estimated from the current in-game sight markings. " +
                        "Fire a spotting round before sustained fire."

                else ->
                    "Target is inside the ${weapon.shortName} effective range. " +
                        "Set the displayed bearing and correct from impact."
            },
            color = if (solution.isInRange) Console.Dim else Console.Alarm,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun ResultCard(label: String, value: String, unit: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .border(1.dp, Console.Edge, RoundedCornerShape(4.dp))
            .background(Console.Panel, RoundedCornerShape(4.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Kicker(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                color = Console.Amber,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            if (unit != null) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    color = Console.Dim,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun OffsetRow(east: Double, north: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Console.Edge, RoundedCornerShape(4.dp))
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${abs(jsRoundToInt(east))} m ${if (east < 0) "WEST" else "EAST"}",
            color = Console.Bright,
            fontSize = 13.sp,
        )
        Text("  +  ", color = Console.Amber, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "${abs(jsRoundToInt(north))} m ${if (north < 0) "SOUTH" else "NORTH"}",
            color = Console.Bright,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun Disclaimer() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Console.Edge))
        Spacer(Modifier.height(4.dp))
        Text(
            text = "L81 range is roughly 120–700 m, SPH-2 735–2,630 m. Mortar elevation is an " +
                "estimate read off the current sight markings; ballistics and weapon limits may " +
                "change as the game is developed.",
            color = Console.Dim,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun Kicker(text: String) {
    Text(
        text = text,
        color = Console.Dim,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0D0B)
@Composable
private fun IdfCalculatorPreview() {
    WardogsIdfTheme {
        Surface(color = Console.Ink) { IdfCalculatorScreen() }
    }
}
