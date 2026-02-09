package bankal_deir.com.Fatora.Data

data class University(
    val id: Int,
    val name: String,
    val type: UniversityType
)

enum class UniversityType {
    PUBLIC,
    PRIVATE,
    INSTITUTE
}