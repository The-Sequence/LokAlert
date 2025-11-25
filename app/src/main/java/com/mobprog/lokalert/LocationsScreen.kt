package com.mobprog.lokalert

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FavoritesScreen(
    recentSearches: List<String>,
    favoriteLocations: List<String>,
    onToggleFavorite: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp), // Adjust padding for a cleaner look
    ) {
        // Main title for the screen
        Text(
            "My Locations",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        // First Section/Row: Search History
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text(
                "Recent Search History",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Scrollable and constrained history list using LazyColumn
            if (recentSearches.isEmpty()) {
                Text("No recent searches.", color = Color.LightGray)
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp), // Limit height to about 5-6 items, enabling scroll if more exist
                    userScrollEnabled = true,
                ) {
                    itemsIndexed(recentSearches) { index, location ->
                        val isFavorite = favoriteLocations.contains(location)
                        SearchHistoryItem(
                            location = location,
                            isFavorite = isFavorite,
                            onToggleFavorite = onToggleFavorite
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // Separator space between the two main sections

        // Second Section/Row: Favorite Locations Content
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start // Align text to start for better layout when list is populated
        ) {
            Text(
                "Favorite Locations",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (favoriteLocations.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("You haven't added any favorite locations yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth() // Use LazyColumn for Favorites List
                ) {
                    items(favoriteLocations) { location ->
                        // Reuse SearchHistoryItem for display, as it now handles the favorite/unfavorite logic
                        SearchHistoryItem(
                            location = location,
                            isFavorite = true, // It is a favorite if it's in this list
                            onToggleFavorite = onToggleFavorite
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}