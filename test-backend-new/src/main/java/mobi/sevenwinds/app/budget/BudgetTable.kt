package mobi.sevenwinds.app.budget

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
//import org.jetbrains.exposed.sql.references

// 3.3 В BudgetTable добавим опциональную привязку по Author.id
object BudgetTable : IntIdTable("budget") {
    val year = integer("year")
    val month = integer("month")
    val amount = integer("amount")
    val type = enumerationByName("type", 100, BudgetType::class)

    //3.2
    val author = Author

// 3.3  Опциональная привязка к таблице Author
//   val authorId = long("author_id").references(Author.id).nullable()
}

class BudgetEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BudgetEntity>(BudgetTable)

    var year by BudgetTable.year
    var month by BudgetTable.month
    var amount by BudgetTable.amount
    var type by BudgetTable.type

    // 3.2
    var author by BudgetTable.author.name


    fun toResponse(): BudgetRecord {
        return BudgetRecord(year, month, amount, type, author = Author)
    }
}

// 3.1  Добавим таблицу Author в схему нашей базы данных со столбцами:
// ID , ФИО  и дата создания (datetime).
object Author: IntIdTable("author") {
    val name = varchar("name", 100)
    val creationDate = datetime("creation_date")
}