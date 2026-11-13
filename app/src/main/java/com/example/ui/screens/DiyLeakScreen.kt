package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.DiyEvaluation
import com.example.data.catalog.WaterTableCatalogs

@Composable
fun DiyLeakScreen(
    diyResults: Map<String, String>,
    onAnswerChange: (stepId: String, value: String) -> Unit,
    onNavigateToContractors: (DiyEvaluation) -> Unit,
    onNavigateToWholesale: () -> Unit
) {
    val evaluation = remember(diyResults) {
        WaterTableCatalogs.evaluateDiyTestResults(diyResults)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "DIY Leak vs. Groundwater Tests",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Run these simple home tests to tell a pressurized plumbing leak from natural ground water table—before spending money.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Diagnostic Verdict Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = evaluation.confidenceBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = evaluation.headline,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = evaluation.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    evaluation.nextSteps.forEach { step ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToContractors(evaluation) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("diy_contact_contractor_button")
                        ) {
                            Icon(Icons.Filled.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Contact Pro")
                        }
                        FilledTonalButton(
                            onClick = onNavigateToWholesale,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("diy_open_wholesale_button")
                        ) {
                            Icon(Icons.Filled.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DIY Supply Kit")
                        }
                    }
                }
            }
        }

        // Interactive Steps
        items(WaterTableCatalogs.diyLeakProtocols, key = { it.id }) { step ->
            val current = diyResults[step.id] ?: "untested"
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Text(
                            text = step.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = step.durationLabel,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        text = step.instructions,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("• Positive (+): ${step.positiveMeans}", style = MaterialTheme.typography.bodySmall)
                            Text("• Negative (-): ${step.negativeMeans}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = current == "yes",
                            onClick = { onAnswerChange(step.id, "yes") },
                            label = { Text("Positive (+)", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = current == "no",
                            onClick = { onAnswerChange(step.id, "no") },
                            label = { Text("Negative (-)", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = current == "untested",
                            onClick = { onAnswerChange(step.id, "untested") },
                            label = { Text("Untested") }
                        )
                    }
                }
            }
        }
    }
}
