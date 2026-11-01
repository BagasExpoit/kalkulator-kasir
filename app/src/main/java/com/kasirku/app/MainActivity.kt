package com.kasirku.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PointOfSale
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: KasirViewModel = viewModel()
            KasirTheme(vm.darkTheme) { KasirApp(vm) }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Kasir("Kasir", Icons.Rounded.PointOfSale),
    Toko("Toko", Icons.Rounded.Storefront),
    Qris("QRIS", Icons.Rounded.QrCode2),
    Laporan("Laporan", Icons.Rounded.ReceiptLong),
    Dev("Pengembang", Icons.Rounded.Code)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirApp(vm: KasirViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { msg ->
        scope.launch { snack.currentSnackbarData?.dismiss(); snack.showSnackbar(msg) }
    }
    BackHandler(enabled = tab != 0) { tab = 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    androidx.compose.foundation.layout.Column {
                        Text(
                            vm.storeName.ifBlank { "KasirKu" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, maxLines = 1
                        )
                        Text(
                            Tab.entries[tab].label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleTheme() }) {
                        Icon(
                            if (vm.darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "Ganti tema"
                        )
                    }
                    SaveButton(vm.dirty) { vm.save(); notify("Tersimpan ✓") }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            // Badge jumlah invoice di tab Laporan
                            if (t == Tab.Laporan && vm.invoices.isNotEmpty()) {
                                BadgedBox(badge = {
                                    Badge {
                                        val count = vm.invoices.size
                                        Text(if (count > 99) "99+" else "$count")
                                    }
                                }) { Icon(t.icon, contentDescription = t.label) }
                            } else {
                                Icon(t.icon, contentDescription = t.label)
                            }
                        },
                        label = { Text(t.label) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snack) }
    ) { pad ->
        Box(Modifier.padding(pad).consumeWindowInsets(pad).imePadding()) {
            Crossfade(targetState = tab, label = "tab") { t ->
                when (t) {
                    0 -> KasirScreen(vm, notify)
                    1 -> TokoScreen(vm, notify)
                    2 -> QrisScreen(vm, notify)
                    3 -> LaporanScreen(vm)
                    else -> DevScreen()
                }
            }
        }
    }
}

@Composable
private fun SaveButton(dirty: Boolean, onClick: () -> Unit) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(Icons.Rounded.Save, null, Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Simpan")
    }
    val pad = PaddingValues(horizontal = 14.dp)
    if (dirty) Button(onClick = onClick, contentPadding = pad, content = content)
    else FilledTonalButton(onClick = onClick, contentPadding = pad, content = content)
}
