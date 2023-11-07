package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { BudgetTable.deleteAll() }
    }


    // 1. Задача: Починить тест
    // 1.1 * `testBudgetPagination` - некорректно работает пагинация + неправильно считается общая статистика записей
    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход, author = Author))

        RestAssured.given()
            .queryParam("limit", 6) // 1.1 Исправление: было 3,  установил лимит 6, чтобы получить все записи
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(6, response.total) // 1.1 Исправление: было 5, общее количество записей должно быть равно 6
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход, author = Author))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }


    // 1.2 Задача: Починить тест
    // 1.2 * `testStatsSortOrder` - необходимо реализовать сортировку выдачи в указанном порядке
    // 1.2 Исправленный тест: ожидаемый порядок сортировки - сначала по возрастанию месяца,
    @Test
    fun testStatsSortOrderNew() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход, author = Author))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход, author = Author))

        // expected sort order - month ascending, amount descending

        val expectedOrder = listOf(
            BudgetRecord(2020, 1, 30, BudgetType.Приход, author = Author),
            BudgetRecord(2020, 1, 5, BudgetType.Приход, author = Author),
            BudgetRecord(2020, 5, 400, BudgetType.Приход, author = Author),
            BudgetRecord(2020, 5, 100, BudgetType.Приход, author = Author),
            BudgetRecord(2020, 5, 50, BudgetType.Приход, author = Author)
        )

        val response = RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>()

        Assert.assertEquals(expectedOrder, response.items)


        // 1.2 затем по убыванию суммы
        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&sort=amount:desc")
            .toResponse<BudgetYearStatsResponse>()
            .let { response ->
                println(response.items)

                Assert.assertEquals(400, response.items[0].amount)
                Assert.assertEquals(100, response.items[1].amount)
                Assert.assertEquals(50, response.items[2].amount)
                Assert.assertEquals(30, response.items[3].amount)
                Assert.assertEquals(5, response.items[4].amount)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetRecord(2020, -5, 5, BudgetType.Приход, author = Author))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetRecord(2020, 15, 5, BudgetType.Приход, author = Author))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetRecord) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>().let { response ->
                Assert.assertEquals(record, response)
            }
    }
}