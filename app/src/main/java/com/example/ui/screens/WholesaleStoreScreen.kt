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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.ContractorProductReview
import com.example.data.model.EquipmentRentalItem
import com.example.data.model.JobKitRecommendation
import com.example.data.model.SupplyOrder
import com.example.data.model.WholesaleProduct
import com.example.ui.UiState
import com.example.ui.WaterTableViewModel
import com.example.ui.components.RatingStarsRow
import com.example.ui.theme.FloodCrimson
import com.example.ui.theme.SafeEmerald
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WholesaleStoreScreen(
    viewModel: WaterTableViewModel,
    kitRecommendationState: UiState<JobKitRecommendation>?,
    cartItems: List<CartItem>,
    supplyOrdersUi: UiState<List<SupplyOrder>>,
    equipmentRentalsUi: UiState<List<EquipmentRentalItem>> = UiState.Success(emptyList()),
    productReviewsUi: UiState<List<ContractorProductReview>> = UiState.Success(emptyList()),
    defaultCity: String,
    isSubmitting: Boolean,
    onOpenVideoStudioWithProduct: (titleAndDesc: String) -> Unit
) {
    var supplyTab by remember { mutableIntStateOf(0) } // 0 = Wholesale, 1 = Equipment Rentals, 2 = Pro Field Reviews
    var jobDescription by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showCheckoutDialog by remember { mutableStateOf(false) }

    // Equipment rentals UI state
    var rentalCategoryFilter by remember { mutableStateOf("All") }
    var rentalCityFilter by remember { mutableStateOf("") }
    var showListRentalDialog by remember { mutableStateOf(false) }

    // Product reviews UI state
    var reviewUseCaseFilter by remember { mutableStateOf("all") }
    var showWriteReviewDialog by remember { mutableStateOf(false) }
    var reviewTargetProduct by remember { mutableStateOf<WholesaleProduct?>(null) }

    val categories = listOf("All", "French Drain", "Sump Pumps", "Fabric & Gravel", "Fittings", "Waterproofing")
    val filteredProducts = remember(selectedCategory) {
        if (selectedCategory == "All") WaterTableCatalogs.wholesaleProducts
        else WaterTableCatalogs.wholesaleProducts.filter { it.category == selectedCategory }
    }

    val rentals = (equipmentRentalsUi as? UiState.Success)?.data.orEmpty()
    val filteredRentals = remember(rentals, rentalCategoryFilter, rentalCityFilter) {
        rentals.filter { item ->
            val catOk = rentalCategoryFilter == "All" || item.category.equals(rentalCategoryFilter, ignoreCase = true)
            val cityOk = rentalCityFilter.isBlank() || item.city.contains(rentalCityFilter, ignoreCase = true)
            catOk && cityOk
        }
    }

    val productReviews = (productReviewsUi as? UiState.Success)?.data.orEmpty()
    val filteredProductReviews = remember(productReviews, reviewUseCaseFilter) {
        if (reviewUseCaseFilter == "all") productReviews
        else productReviews.filter { it.disasterUseCase == reviewUseCaseFilter }
    }

    val cartWholesaleTotal = cartItems.sumOf { it.product.wholesalePriceUsd * it.quantity }
    val cartRetailTotal = cartItems.sumOf { it.product.retailBenchmarkUsd * it.quantity }
    val cartSavings = (cartRetailTotal - cartWholesaleTotal).coerceAtLeast(0.0)
    val orders = (supplyOrdersUi as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = supplyTab) {
            Tab(
                selected = supplyTab == 0,
                onClick = { supplyTab = 0 },
                text = { Text("Wholesale") },
                modifier = Modifier.testTag("supply_tab_wholesale")
            )
            Tab(
                selected = supplyTab == 1,
                onClick = { supplyTab = 1 },
                text = { Text("Rentals (${rentals.size})") },
                modifier = Modifier.testTag("supply_tab_rentals")
            )
            Tab(
                selected = supplyTab == 2,
                onClick = { supplyTab = 2 },
                text = { Text("Pro Reviews (${productReviews.size})") },
                modifier = Modifier.testTag("supply_tab_reviews")
            )
        }

        when (supplyTab) {
            // TAB 0: WHOLESALE DIRECT SUPPLY
            0 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                text = "Save 25–38% vs. Big-Box Retail Benchmarks. Plus tap 'Rentals' for nearby dehumidifiers/HEPA gear or 'Pro Reviews' for honest contractor tool tests.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

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

                items(filteredProducts, key = { it.sku }) { product ->
                    WholesaleProductCard(
                        product = product,
                        reviewsCount = productReviews.count { it.productSku == product.sku },
                        onAdd = { viewModel.addProductToCart(product) },
                        onReview = {
                            reviewTargetProduct = product
                            showWriteReviewDialog = true
                        },
                        onCreateVideoAd = {
                            onOpenVideoStudioWithProduct(
                                "${product.name} (${product.category}, ${product.unitLabel}) — $${String.format(Locale.US, "%.2f", product.wholesalePriceUsd)} wholesale vs $${String.format(Locale.US, "%.2f", product.retailBenchmarkUsd)} big-box retail (${product.savingsPercent}% off). ${product.specs}"
                            )
                        }
                    )
                }

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

            // TAB 1: NEARBY REMEDIATION EQUIPMENT RENTALS (Restoration Cos & Neighbors)
            1 -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.Handyman, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Nearby Remediation Equipment Rentals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Rent LGR dehumidifiers, HEPA negative air scrubbers, high-velocity air movers & moisture meters directly from local restoration companies or neighbors to dry & remediate properly.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = { showListRentalDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("list_equipment_rental_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("List Equipment for Nearby Renters")
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = rentalCityFilter,
                        onValueChange = { rentalCityFilter = it },
                        label = { Text("Filter by city (default shows all nearby)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Dehumidifier", "Air Scrubber", "Air Mover", "Moisture Meter", "Sump Pump", "Thermal Camera").forEach { cat ->
                            FilterChip(
                                selected = rentalCategoryFilter == cat,
                                onClick = { rentalCategoryFilter = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }

                if (filteredRentals.isEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Text(
                                text = "No remediation rentals match right now. Local restoration companies & neighbors can list idle dehumidifiers or HEPA scrubbers above.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filteredRentals, key = { it.id }) { rental ->
                        val isOwner = rental.ownerUserId == viewModel.currentUserId
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rental.equipmentName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            text = "${rental.category} · ${rental.city} · ${rental.companyName.ifBlank { rental.ownerName }} (${rental.ownerType.replace('_', ' ')})",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = if (rental.isAvailable) Color(0xFFD1FAE5) else Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = if (rental.isAvailable) "$${rental.dailyRateUsd}/day" else "RENTED OUT",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (rental.isAvailable) SafeEmerald else FloodCrimson,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                if (rental.specs.isNotBlank()) Text(rental.specs, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    text = "Pickup / Deposit: $${rental.depositUsd} · Contact: ${rental.contactPhoneOrNote.ifBlank { "Message via Pros tab" }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (isOwner) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedButton(
                                            onClick = { viewModel.toggleEquipmentRentalAvailability(rental.id, !rental.isAvailable) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(if (rental.isAvailable) "Mark Rented Out" else "Mark Available")
                                        }
                                        IconButton(onClick = { viewModel.deleteEquipmentRental(rental.id) }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete rental listing", tint = FloodCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TAB 2: REAL CONTRACTOR FIELD PRODUCT REVIEWS
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.RateReview, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Real Contractor Field Product Reviews", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Honest jobsite performance reviews on pumps, membranes, antimicrobials, dehumidifiers & pipes from verified contractors.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Button(
                                onClick = {
                                    reviewTargetProduct = null
                                    showWriteReviewDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("write_product_review_button")
                            ) {
                                Text("Post Honest Product Field Review")
                            }
                        }
                    }
                }

                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "all" to "All Disasters",
                            "flooding" to "Flooding",
                            "mold_cleanse" to "Mold Cleanse",
                            "wildfire_smoke" to "Wildfire Smoke",
                            "groundwater" to "Groundwater"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = reviewUseCaseFilter == key,
                                onClick = { reviewUseCaseFilter = key },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                if (filteredProductReviews.isEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Text(
                                text = "No product field reviews yet for this filter. Contractors can log real product pros, cons & job counts above.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(filteredProductReviews, key = { it.id }) { rev ->
                        Card(shape = RoundedCornerShape(16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rev.productName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text("${rev.brandModel.ifBlank { rev.category }} · Used on ${rev.jobsUsedCount} jobs (${rev.disasterUseCase.replace('_', ' ')})", style = MaterialTheme.typography.labelSmall)
                                    }
                                    RatingStarsRow("Field", rev.rating.toDouble())
                                }
                                Text(rev.honestReview, style = MaterialTheme.typography.bodySmall)
                                if (rev.pros.isNotBlank()) Text("Pros: ${rev.pros}", style = MaterialTheme.typography.labelSmall, color = SafeEmerald)
                                if (rev.cons.isNotBlank()) Text("Cons: ${rev.cons}", style = MaterialTheme.typography.labelSmall, color = FloodCrimson)
                                Text(
                                    text = "Reviewed by ${rev.contractorName}${if (rev.licenseNumber.isNotBlank()) " (Lic #${rev.licenseNumber})" else ""} · ${if (rev.wouldUseOnInsuredJob) "Insured-Job Approved" else "Not Recommended for Claims"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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

    if (showListRentalDialog) {
        ListEquipmentRentalDialog(
            defaultCity = defaultCity,
            isSubmitting = isSubmitting,
            onDismiss = { showListRentalDialog = false },
            onSubmit = { ownerType, company, eqName, cat, city, rate, dep, specs, contact ->
                viewModel.listEquipmentForRent(ownerType, company, eqName, cat, city, rate, dep, specs, contact) {
                    showListRentalDialog = false
                }
            }
        )
    }

    if (showWriteReviewDialog) {
        WriteProductReviewDialog(
            preselectedProduct = reviewTargetProduct,
            isSubmitting = isSubmitting,
            onDismiss = { showWriteReviewDialog = false },
            onSubmit = { cName, company, lic, sku, pName, brand, cat, disaster, rating, jobs, pros, cons, honest, insured ->
                viewModel.createContractorProductReview(
                    cName, company, lic, sku, pName, brand, cat, disaster, rating, jobs, pros, cons, honest, insured
                ) {
                    showWriteReviewDialog = false
                    supplyTab = 2
                }
            }
        )
    }
}

@Composable
private fun WholesaleProductCard(
    product: WholesaleProduct,
    reviewsCount: Int,
    onAdd: () -> Unit,
    onReview: () -> Unit,
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
                        onClick = onReview,
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(if (reviewsCount > 0) "Rev ($reviewsCount)" else "Review", maxLines = 1)
                    }
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
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("Delivery City / Town") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Jobsite Street Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = job, onValueChange = { job = it }, label = { Text("Job Notes (Optional)") }, modifier = Modifier.fillMaxWidth())
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = String.format(
                            Locale.US,
                            "Wholesale Total: $%.2f\nBig-Box Benchmark: ~$%.2f (You save $%.2f)\nPlatform Update Fee (5%%): $%.2f",
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
            ) { Text("Place Order") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListEquipmentRentalDialog(
    defaultCity: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ownerType: String, company: String, eqName: String, cat: String, city: String, rate: Int, dep: Int, specs: String, contact: String) -> Unit
) {
    var ownerType by remember { mutableStateOf("restoration_company") }
    var companyName by remember { mutableStateOf("") }
    var eqName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Dehumidifier") }
    var city by remember { mutableStateOf(defaultCity) }
    var rateText by remember { mutableStateOf("45") }
    var depositText by remember { mutableStateOf("100") }
    var specs by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("List Remediation Equipment") },
        text = {
            LazyColumn(modifier = Modifier.height(390.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = ownerType == "restoration_company", onClick = { ownerType = "restoration_company" }, label = { Text("Restoration Co.") })
                        FilterChip(selected = ownerType == "resident", onClick = { ownerType = "resident" }, label = { Text("Neighbor / Owner") })
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Dehumidifier", "Air Scrubber", "Air Mover", "Moisture Meter", "Sump Pump", "Thermal Camera").forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat) })
                        }
                    }
                    OutlinedTextField(value = eqName, onValueChange = { eqName = it }, label = { Text("Equipment Name (e.g., Dri-Eaz LGR 7000XLi)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Company or Neighbor Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1.2f))
                        OutlinedTextField(value = rateText, onValueChange = { rateText = it.filter { c -> c.isDigit() } }, label = { Text("$/Day") }, singleLine = true, modifier = Modifier.weight(0.7f))
                        OutlinedTextField(value = depositText, onValueChange = { depositText = it.filter { c -> c.isDigit() } }, label = { Text("Dep $") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    }
                    OutlinedTextField(value = specs, onValueChange = { specs = it }, label = { Text("Specs / filters / sanitization included") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Pickup hours / contact note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(ownerType, companyName, eqName, category, city, rateText.toIntOrNull() ?: 45, depositText.toIntOrNull() ?: 100, specs, contact) },
                enabled = !isSubmitting && eqName.trim().length >= 2
            ) { Text("Publish Rental") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WriteProductReviewDialog(
    preselectedProduct: WholesaleProduct?,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        cName: String, company: String, lic: String, sku: String, pName: String, brand: String,
        cat: String, disaster: String, rating: Int, jobs: Int, pros: String, cons: String, honest: String, insured: Boolean
    ) -> Unit
) {
    var contractorName by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var license by remember { mutableStateOf("") }
    var productName by remember(preselectedProduct) { mutableStateOf(preselectedProduct?.name ?: "") }
    var brandModel by remember { mutableStateOf("") }
    var category by remember(preselectedProduct) { mutableStateOf(preselectedProduct?.category ?: "Waterproofing") }
    var disasterUseCase by remember { mutableStateOf("flooding") }
    var rating by remember { mutableIntStateOf(5) }
    var jobsText by remember { mutableStateOf("5") }
    var pros by remember { mutableStateOf("") }
    var cons by remember { mutableStateOf("") }
    var honestReview by remember { mutableStateOf("") }
    var insuredJobOk by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Post Honest Field Product Review") },
        text = {
            LazyColumn(modifier = Modifier.height(400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(value = productName, onValueChange = { productName = it }, label = { Text("Product Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = brandModel, onValueChange = { brandModel = it }, label = { Text("Brand & Model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = contractorName, onValueChange = { contractorName = it }, label = { Text("Contractor Name") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = license, onValueChange = { license = it }, label = { Text("Lic #") }, singleLine = true, modifier = Modifier.weight(0.7f))
                    }
                    OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("flooding" to "Flood", "mold_cleanse" to "Mold", "wildfire_smoke" to "Wildfire/Smoke", "groundwater" to "Groundwater").forEach { (k, l) ->
                            FilterChip(selected = disasterUseCase == k, onClick = { disasterUseCase = k }, label = { Text(l) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { s ->
                            FilledTonalButton(onClick = { rating = s }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(0.dp)) {
                                Text(if (rating == s) "$s★" else "$s")
                            }
                        }
                    }
                    OutlinedTextField(value = jobsText, onValueChange = { jobsText = it.filter { c -> c.isDigit() } }, label = { Text("Jobs Used On") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pros, onValueChange = { pros = it }, label = { Text("What held up well (Pros)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = cons, onValueChange = { cons = it }, label = { Text("Failures / drawbacks (Cons)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = honestReview, onValueChange = { honestReview = it }, label = { Text("Honest field verdict") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Would use on an insured job?", style = MaterialTheme.typography.bodySmall)
                        Switch(checked = insuredJobOk, onCheckedChange = { insuredJobOk = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        contractorName.ifBlank { "Verified Pro" }, company, license,
                        preselectedProduct?.sku ?: "custom", productName, brandModel, category,
                        disasterUseCase, rating, jobsText.toIntOrNull() ?: 1, pros, cons, honestReview, insuredJobOk
                    )
                },
                enabled = !isSubmitting && productName.trim().length >= 2 && honestReview.trim().length >= 5
            ) { Text("Post Review") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
