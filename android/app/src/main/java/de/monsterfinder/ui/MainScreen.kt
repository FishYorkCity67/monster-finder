package de.monsterfinder.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.monsterfinder.data.Deal
import de.monsterfinder.data.SearchResponse
import de.monsterfinder.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RADII = listOf(2, 5, 10, 20)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(containerColor = Ink, contentWindowInsets = WindowInsets.safeDrawing) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(onSettings = { showSettings = true })
            SearchBar(state, vm)
            PullToRefreshBox(
                isRefreshing = state.loading && state.response != null,
                onRefresh = vm::search,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.error != null -> Message(Icons.Outlined.ErrorOutline, state.error!!, isError = true) {
                        TextButton(onClick = vm::search) { Text("Nochmal versuchen") }
                        if ("erreichbar" in state.error!!) TextButton(onClick = { showSettings = true }) { Text("Server einstellen") }
                    }
                    state.loading && state.response == null -> Loading()
                    state.response != null -> Results(state.response!!)
                    else -> Message(Icons.Outlined.LocalDrink, "Gib deine Postleitzahl ein – ich suche die günstigste Monster-Dose in deiner Nähe.")
                }
            }
        }
    }

    if (showSettings) SettingsSheet(state, vm, onDismiss = { showSettings = false })
}

@Composable
private fun Header(onSettings: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Neon)) { append("MONSTER") }
                append(" FINDER")
            },
            fontSize = 22.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Einstellungen", tint = TextMid) }
    }
}

@Composable
private fun SearchBar(state: UiState, vm: MainViewModel) {
    val focus = LocalFocusManager.current
    Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
        OutlinedTextField(
            value = state.plz,
            onValueChange = vm::onPlzChange,
            placeholder = { Text("Postleitzahl") },
            leadingIcon = { Icon(Icons.Outlined.Place, null, tint = Neon) },
            trailingIcon = {
                if (state.loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                else IconButton(onClick = { focus.clearFocus(); vm.search() }) { Icon(Icons.Outlined.Search, "Suchen") }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp, letterSpacing = 3.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); vm.search() }),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Neon,
                unfocusedBorderColor = Outline,
                focusedContainerColor = Surface1,
                unfocusedContainerColor = Surface1,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RADII.forEach { km ->
                FilterChip(
                    selected = state.radiusKm == km,
                    onClick = { vm.onRadiusChange(km) },
                    label = { Text("$km km") },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Neon,
                        selectedLabelColor = Ink,
                        containerColor = Surface1,
                        labelColor = TextMid,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true, selected = state.radiusKm == km,
                        borderColor = Outline, selectedBorderColor = Neon,
                    ),
                )
            }
        }
    }
}

@Composable
private fun Results(r: SearchResponse) {
    if (r.results.isEmpty()) {
        Message(Icons.Outlined.SearchOff, "Keine Monster-Angebote im Umkreis von ${r.radiusKm.toInt()} km gefunden. Versuch einen größeren Radius.")
        return
    }
    val best = r.results.first()
    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "${r.location.plz} ${r.location.city} · ${r.results.size} Treffer",
                color = TextMid, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        item { BestDealCard(best, r.pfand) }
        if (r.results.size > 1) {
            item { SectionLabel("WEITERE ANGEBOTE") }
            items(r.results.drop(1)) { DealRow(it, best.pricePerCan) }
        }
        item { SourcesFooter(r) }
    }
}

@Composable
private fun BestDealCard(deal: Deal, pfand: Double) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Neon.copy(alpha = 0.22f), Surface1)))
            .border(1.dp, Neon.copy(alpha = 0.55f), shape)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("GÜNSTIGSTER DEAL", color = Neon, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            if (!deal.isOffer) Tag("Richtpreis", TextMid)
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChainBadge(deal.chain, deal.chainName, 44)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(deal.chainName, style = MaterialTheme.typography.headlineSmall)
                Text(deal.title, color = TextMid, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(euro(deal.pricePerCan), style = MaterialTheme.typography.displaySmall)
            Text(" / Dose", color = TextMid, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            "+ ${euro(pfand)} Pfand = ${euro(deal.pricePerCanWithPfand)}" +
                if (deal.cans > 1) " · ${deal.cans}er-Pack für ${euro(deal.price)}" else "",
            color = TextMid, style = MaterialTheme.typography.bodyMedium,
        )
        deal.store?.let { s ->
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Outline)
            Spacer(Modifier.height(14.dp))
            InfoLine(Icons.Outlined.Storefront, listOf(s.name, s.address).filter { it.isNotBlank() }.distinct().joinToString(" · "))
            InfoLine(Icons.Outlined.NearMe, "${km(s.distanceKm)} entfernt")
        }
        validity(deal)?.let { InfoLine(Icons.Outlined.Event, it) }
        deal.store?.let { s ->
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.lat},${s.lon}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Neon, contentColor = Ink),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
            ) {
                Icon(Icons.Outlined.Directions, null)
                Spacer(Modifier.width(8.dp))
                Text("Route starten", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DealRow(deal: Deal, bestPrice: Double) {
    val context = LocalContext.current
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Outline),
        modifier = Modifier.fillMaxWidth().clickable(enabled = deal.store != null) {
            deal.store?.let { s ->
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${s.lat},${s.lon}")))
            }
        },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ChainBadge(deal.chain, deal.chainName, 38)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deal.chainName, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(6.dp))
                    if (!deal.isOffer) Tag("Richtpreis", TextMid)
                    else if (deal.cans > 1) Tag("${deal.cans}er", Neon)
                }
                val sub = listOfNotNull(deal.store?.let { km(it.distanceKm) }, validity(deal)).joinToString(" · ")
                if (sub.isNotEmpty()) Text(sub, color = TextMid, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(euro(deal.pricePerCan), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                val diff = deal.pricePerCan - bestPrice
                Text(if (diff > 0.004) "+${euro(diff)}" else "/ Dose", color = if (diff > 0.004) Warn else TextMid, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ChainBadge(chain: String, name: String, sizeDp: Int) {
    val bg = chainColor(chain)
    Box(
        Modifier.size(sizeDp.dp).clip(CircleShape).background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            color = if (bg.luminance() > 0.5f) Color.Black else Color.White,
            fontWeight = FontWeight.Black,
            fontSize = (sizeDp * 0.42f).sp,
        )
    }
}

@Composable
private fun Tag(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
private fun InfoLine(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextMid, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SectionLabel(text: String) =
    Text(text, color = TextMid, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 14.dp, bottom = 2.dp))

@Composable
private fun SourcesFooter(r: SearchResponse) {
    Column(Modifier.padding(top = 18.dp)) {
        SectionLabel("DATENQUELLEN")
        r.sources.forEach { (k, v) ->
            Text("$k: $v", color = TextMid, fontSize = 12.sp)
        }
        Text("Preise ohne Gewähr · Richtpreise sind Schätzwerte", color = TextMid.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun Loading() {
    Column(Modifier.fillMaxSize().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = Neon)
        Spacer(Modifier.height(16.dp))
        Text("Durchsuche Prospekte & Filialen …", color = TextMid)
    }
}

@Composable
private fun Message(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    isError: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
) {
    // LazyColumn statt Column, damit Pull-to-Refresh auch hier funktioniert
    LazyColumn(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(Modifier.padding(horizontal = 40.dp, vertical = 70.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, tint = if (isError) MaterialTheme.colorScheme.error else Neon, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(16.dp))
                Text(text, color = TextMid, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Row(Modifier.padding(top = 8.dp), content = actions)
            }
        }
    }
}

private fun euro(v: Double) = String.format(Locale.GERMANY, "%.2f €", v)

private fun km(v: Double) = if (v < 1) "${(v * 1000).toInt()} m" else String.format(Locale.GERMANY, "%.1f km", v)

private fun validity(d: Deal): String? {
    val to = d.validTo?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), to)
    return when {
        days < 0 -> "abgelaufen"
        days == 0L -> "nur noch heute"
        else -> "bis " + to.format(DateTimeFormatter.ofPattern("EE dd.MM.", Locale.GERMANY))
    }
}
