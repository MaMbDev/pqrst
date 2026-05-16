package dam.pmdm.pqrstlearn.presentation.tour

/**
 * A single step in the interactive app tour.
 *
 * @param targetKey  Key used to look up the highlighted element's screen bounds.
 * @param titleRes   String resource ID for the tooltip title.
 * @param descRes    String resource ID for the tooltip description.
 * @param route      If non-null, navigate to this route when this step becomes active.
 * @param tooltipBelow  True → tooltip appears below the spotlight; false → above.
 */
data class TourStep(
    val targetKey: String,
    val titleRes: Int,
    val descRes: Int,
    val route: Any? = null,
    val tooltipBelow: Boolean = true,
)
