package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.data.catalog.WaterTableCatalogs
import com.example.data.model.CartItem
import com.example.data.model.JobKitRecommendation
import com.example.data.model.SupplyOrder
import com.example.data.model.WholesaleProduct
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.theme.SafeEmerald
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WholesaleStoreScreen(
    viewModel: WaterTableViewModel,
    kitRecommendationState: UiState<JobKitRecommendation>?,
    cartItems: List<CartItem>,
    supplyOrdersUi: UiState<List<SupplyOrder>>,
    defaultCity: String,
    isSubmitting: Boolean,
    onOpenVideoStudioWithProduct: (titleAndDesc: String) -> Unit
) {
    var jobDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    val categories = listOf("All", "French Drain", "Sump Pumps", "Fabric & Gravel", "Fittings", "Waterproofing")
    val filteredProducts = remember(selectedCategory) {
        if (selectedCategory == "All") WaterTableCatalogs.wholesaleProducts
        else WaterTableCatalogs.wholesaleProducts.filter { it.category == selectedCategory }
    }

    val cartWholesaleTotal = cartItems.sumOf { it.product.wholesalePriceUsd * it.quantity }
    val cartRetailTotal = cartItems.sumOf { it.product.retailBenchmarkUsd * it.quantity }
    val cartSavings = (cartRetailTotal - cartWholesaleTotal).coerceAtLeast(0.0)
    val orders = (supplyOrdersUi as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Wholesale Price Advantage Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Color.White)
                        Text(
                            text = "Wholesale Direct Drop-Ship Supply",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Save 25–38% vs. Home Depot & Big-Box Retail Benchmarks. Direct warehouse shipping on French drain pipe, geotextile fabric, cast-iron sump pumps & check valves.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }
        }

        // AI Job-to-Materials Kit Builder
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text("AI Job-to-Materials Kit Builder", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Describe the task you need fulfilled (e.g., \"80 ft clay-soil French drain along garage + battery-backup sump pump\") and get a complete drop-ship bill of materials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = jobDescription,
                        onValueChange = { jobDescription = it },
                        label = { Text("Describe drainage or sump job") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wholesale_job_input")
                    )
                    Button(
                        onClick = { viewModel.generateWholesaleJobKit(jobDescription) },
                        enabled = jobDescription.trim().length >= 8 && kitRecommendationState !is UiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("build_job_kit_button")
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Build Wholesale Drop-Ship Kit")
                    }

                    when (kitRecommendationState) {
                        UiState.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        is UiState.Error -> Text(kitRecommendationState.message, color = MaterialTheme.colorScheme.error)
                        is UiState.Success -> {
                            val rec = kitRecommendationState.data
                            val catalogMap = WaterTableCatalogs.wholesaleProducts.associateBy { it.sku }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(rec.kitSummary, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = String.format(Locale.US, "Estimated Savings vs. Big-Box Retail: $%.2f", rec.estimatedSavingsVsRetailUsd),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SafeEmerald
                                    )
                                    rec.recommendedSkus.forEach { (sku, qty) ->
                                        val p = catalogMap[sku]
                                        if (p != null) {
                                            Text(
                                                text = "• ${qty}x ${p.name} — $${String.format(Locale.US, "%.2f", p.wholesalePriceUsd * qty)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = { viewModel.addRecommendedKitToCart(rec) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Add Full Job Kit to Cart")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val itemList = rec.recommendedSkus.mapNotNull { (sku, qty) ->
                                                catalogMap[sku]?.let { "${qty}x ${it.name}" }
                                            }.joinToString(", ")
                                            onOpenVideoStudioWithProduct(
                                                "Wholesale Drainage Kit ($${String.format(Locale.US, "%.0f", rec.estimatedSavingsVsRetailUsd)} cheaper than retail): ${rec.kitSummary}. Includes: $itemList"
                                            )
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Turn Job Kit into Product Video Ad")
                                    }
                                }
                            }
                        }
                        null -> Unit
                    }
                }
            }
        }

        // Cart Summary Bar
        if (cartItems.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Drop-Ship Cart (${cartItems.sumOf { it.quantity }} items)", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = String.format(Locale.US, "Save $%.2f vs. Retail", cartSavings),
                                style = MaterialTheme.typography.labelLarge,
                                color = SafeEmerald
                            )
                        }
                        cartItems.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${item.product.name} (${item.quantity}x)",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(onClick = { viewModel.updateCartQuantity(item.product.sku, -1) }) { Text("-") }
                                    Text("${item.quantity}", style = MaterialTheme.typography.labelLarge)
                                    TextButton(onClick = { viewModel.updateCartQuantity(item.product.sku, +1) }) { Text("+") }
                                }
                            }
                        }
                        Button(
                            onClick = { showCheckoutDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("checkout_wholesale_button")
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(String.format(Locale.US, "Submit Drop-Ship Order ($%.2f)", cartWholesaleTotal))
                        }
                    }
                }
            }
        }

        // Category Filter Chips
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) }
                    )
                }
            }
        }

        // Wholesale Catalog Items
        items(filteredProducts, key = { it.sku }) { product ->
            WholesaleProductCard(
                product = product,
                onAdd = { viewModel.addProductToCart(product) },
                onCreateVideoAd = {
                    onOpenVideoStudioWithProduct(
                        "${product.name} (${product.category}, ${product.unitLabel}) — $${String.format(Locale.US, "%.2f", product.wholesalePriceUsd)} wholesale vs $${String.format(Locale.US, "%.2f", product.retailBenchmarkUsd)} big-box retail (${product.savingsPercent}% off). ${product.specs}"
                    )
                }
            )
        }

        // Past Drop-Ship Orders
        if (orders.isNotEmpty()) {
            item {
                Text("My Drop-Ship Orders", style = MaterialTheme.typography.titleLarge)
            }
            items(orders, key = { it.id }) { order ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Order · ${order.shippingCity}", style = MaterialTheme.typography.titleSmall)
                            Text(order.status.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Text(order.jobDescription, style = MaterialTheme.typography.bodySmall)
                        Text(
                            text = String.format(
                                Locale.US,
                                "Wholesale $%.2f · Saved $%.2f vs. Big-Box Retail · Platform fee $%.2f",
                                order.wholesaleTotalUsd, order.totalSavingsUsd, order.platformFeeUsd
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = SafeEmerald
                        )
                    }
                }
            }
        }
    }

    if (showCheckoutDialog) {
        WholesaleCheckoutDialog(
            defaultCity = defaultCity,
            jobDescription = jobDescription,
            wholesaleTotal = cartWholesaleTotal,
            retailBenchmark = cartRetailTotal,
            isSubmitting = isSubmitting,
            onDismiss = { showCheckoutDialog = false },
            onSubmit = { city, address, job ->
                viewModel.placeWholesaleSupplyOrder(job, city, address)
                showCheckoutDialog = false
            }
        )
    }
}

@Composable
private fun WholesaleProductCard(
    product: WholesaleProduct,
    onAdd: () -> Unit,
    onCreateVideoAd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${product.category} · ${product.unitLabel} · ${product.shipsFrom}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "-${product.savingsPercent}% vs Retail",
                        style = MaterialTheme.typography.labelSmall,
                        color = SafeEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(product.specs, style = MaterialTheme.typography.bodySmall)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "$%.2f", product.wholesalePriceUsd),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = String.format(Locale.US, "Retail ~$%.2f", product.retailBenchmarkUsd),
                        style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCreateVideoAd,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Filled.Movie, contentDescription = "Create product video ad", modifier = Modifier.size(16.dp))
                    }
                    FilledTonalButton(
                        onClick = onAdd,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(Icons.Filled.AddShoppingCart, contentDescription = "Add to cart", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun WholesaleCheckoutDialog(
    defaultCity: String,
    jobDescription: String,
    wholesaleTotal: Double,
    retailBenchmark: Double,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (city: String, address: String, job: String) -> Unit
) {
    var city by remember { mutableStateOf(defaultCity) }
    var address by remember { mutableStateOf("") }
    var job by remember { mutableStateOf(jobDescription) }
    val platformFee = wholesaleTotal * 0.05
    val savings = (retailBenchmark - wholesaleTotal).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Drop-Ship Order Summary") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Delivery City / Town") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Jobsite Street Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = job,
                    onValueChange = { job = it },
                    label = { Text("Job Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "Wholesale Total: $%.2f\nBig-Box Benchmark: ~$%.2f (You save $%.2f)\nPlatform Update Fee (5%%): $%.2f\n\nOrder is saved to your account; supplier dispatch & card settlement connect via backend webhook.",
                            wholesaleTotal, retailBenchmark, savings, platformFee
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(city, address, job) },
                enabled = !isSubmitting && city.isNotBlank() && address.trim().length >= 4
            ) {
                Text("Place Order")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
