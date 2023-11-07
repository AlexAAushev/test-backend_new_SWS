package mobi.sevenwinds.app.budget

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.annotations.type.number.integer.max.Max
import com.papsign.ktor.openapigen.annotations.type.number.integer.min.Min
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import javassist.NotFoundException
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.flywaydb.core.internal.jdbc.JdbcTemplate
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.joda.time.DateTime
//import io.ktor.application.call
//import com.papsign.ktor.application.call
import io.ktor.client.engine.*
import org.joda.time.LocalDateTime

// 3.5 Задача: Дополнить `/budget/add` возможностью указать ID автора (опциональное поле)
fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<Unit, BudgetRecord, BudgetRecord>(info("Добавить запись")) { param, body ->

            // 3.5 Добавление переменной для опционального ID автора
            val authorId: Long? = null /*call.parameters["author_id"]?.toLongOrNull()*/
            respond(BudgetService.addRecord(body, authorId))
        }

// 3.2 Задача: Добавить в апи метод создания новой записи в `Author`.
// На вход передается ФИО, дата создания проставляется сервером автоматически.
// 3.2
        route("/author/add").post<Unit, BudgetRecord, BudgetRecord>(info("Добавить автора")) { _, body ->
            val authorId = Author.insertAndGetId {
                it[name] = body.author.name.toString()
                it[creationDate] = DateTime.now()
            }
            val author = Author.select { Author.id eq authorId }
                .singleOrNull()
            if (author != null) {
//                respond(BudgetRecord(author.id.value, author.name, author.creationDate))
            } else {
                throw NotFoundException("Автор не найден")
            }
        }

// 3.6 Задача: В элементах ответа `/budget/year/{year}/stats` выводить ФИО автора, если он указан для записи,
//     а также время создания записи автора.

// 3.7 Задача: Добавить в параметры запроса `/budget/year/{year}/stats` опциональный фильтр по ФИО автора и
//     фильтровать по совпадению подстроки игнорируя регистр

        route("/year/{year}/stats") {
            get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
                respond(BudgetService.getYearStats(param))

// 3.6  В этом коде мы обновляем каждую запись в списке items, чтобы добавить ФИО автора и время создания записи,
                // если они указаны. Если ФИО автора или время создания отсутствуют, мы используем значения по умолчанию
                // "Unknown" для ФИО автора и текущее время для времени создания записи.
                // Затем мы возвращаем обновленный объект BudgetYearStatsResponse в ответе.

//                val stats = BudgetService.getYearStats(param)
//
//                val updatedItems = stats.items.map { item ->
//                    val authorName = item.author.name ?: "Unknown"
//                    val creationTime = item.author.creationDate ?: LocalDateTime.now()

//                    item.copy(
//                        authorName = authorName,
//                        creationTime = creationTime
//                    )
//                }
//
//                val updatedStats = stats.copy(items = updatedItems)
//
//                respond(updatedStats)

// 3.7  В данном коде добавили условие для фильтрации записей по автору.
            // Если параметр author указан, то выполняется фильтрация, иначе показываются все записи.
            // В этом условии используем функцию contains для проверки наличия подстроки в ФИО автора,
            // а также устанавливаем параметр ignoreCase = true, чтобы игнорировать регистр при сравнении.

//                val filteredItems = BudgetService.getYearStats(param).items.filter { record ->
//                    param.author?.let { author ->
//                        record.author.contains(author, ignoreCase = true)
//                    } ?: true // Если параметр author не указан, то не выполнять фильтрацию по автору
//                }
//                respond(BudgetYearStatsResponse(filteredItems.size, mapTotalByType(filteredItems), filteredItems))
            }
        }
    }
}

data class BudgetRecord(
    @Min(1900) val year: Int,
    @Min(1) @Max(12) val month: Int,
    @Min(1) val amount: Int,
    val type: BudgetType,

    // 3.2
    val author: Author,
)

data class BudgetYearParam(
    @PathParam("Год") val year: Int,
    @QueryParam("Лимит пагинации") val limit: Int,
    @QueryParam("Смещение пагинации") val offset: Int,

    )

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<BudgetRecord>,
)


// 2.Задача: Из модели BudgetType через миграцию БД убрать значение Комиссия, заменив его на Расход
enum class BudgetType {
    Приход, Расход, Комиссия
}

// 2.
enum class BudgetTypeNew {
    Приход, Расход
}

// 2. Создадим файл миграции базы данных, чтобы обновить существующие данные в базе данных.
// В ходе этой миграции мы обновим все вхождения "Комиссия" на "Расход".
class UpdateBudgetTypeMigration : BaseJavaMigration() {
    override fun migrate(context: Context?) {
        val jdbcTemplate = JdbcTemplate(context?.connection)

        jdbcTemplate.update("UPDATE your_table SET budget_type = 'Расход' WHERE budget_type = 'Комиссия'")
    }
}




