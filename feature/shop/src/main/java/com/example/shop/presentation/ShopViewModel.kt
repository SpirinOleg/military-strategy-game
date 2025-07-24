package com.example.shop.presentation


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.constants.GameConstants
import com.example.common.model.UnitStats
import com.example.common.model.UnitType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ShopUiState(
    val points: Int = GameConstants.INITIAL_POINTS,
    val availableUnits: List<UnitStats> = emptyList(),
    val purchasedUnits: Map<UnitType, Int> = emptyMap()
)

class ShopViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ShopUiState())
    val uiState: StateFlow<ShopUiState> = _uiState.asStateFlow()

    init {
        initializeShop()
    }

    private fun initializeShop() {
        val availableUnits = listOf(
            UnitType.HELICOPTER,
            UnitType.AIRPLANE,
            UnitType.TANK,
            UnitType.FORTIFY_VEHICLE,
            UnitType.BTR,
            UnitType.BMP,
            UnitType.RIFLEMAN,
            UnitType.MACHINE_GUNNER,
            UnitType.ROCKET_SOLDIER,
            UnitType.MISSILE
        ).mapNotNull { GameConstants.UNIT_STATS[it] }

        _uiState.value = _uiState.value.copy(
            availableUnits = availableUnits,
            purchasedUnits = UnitType.entries.associateWith { 0 }
        )
    }

    fun purchaseUnit(unitType: UnitType) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val unitStats = GameConstants.UNIT_STATS[unitType] ?: return@launch

            if (currentState.points >= unitStats.cost) {
                val newPurchasedUnits = currentState.purchasedUnits.toMutableMap()
                newPurchasedUnits[unitType] = (newPurchasedUnits[unitType] ?: 0) + 1

                _uiState.value = currentState.copy(
                    points = currentState.points - unitStats.cost,
                    purchasedUnits = newPurchasedUnits
                )
            }
        }
    }

    fun sellUnit(unitType: UnitType) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val currentQuantity = currentState.purchasedUnits[unitType] ?: 0

            if (currentQuantity > 0) {
                val unitStats = GameConstants.UNIT_STATS[unitType] ?: return@launch
                val newPurchasedUnits = currentState.purchasedUnits.toMutableMap()
                newPurchasedUnits[unitType] = currentQuantity - 1

                _uiState.value = currentState.copy(
                    points = currentState.points + unitStats.cost / 2, // Продаем за полцены
                    purchasedUnits = newPurchasedUnits
                )
            }
        }
    }
}