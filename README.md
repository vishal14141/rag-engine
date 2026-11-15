# RBI Annual Report - RAG Application

Spring Boot based RAG web application to interact with RBI Annual Reports using natural language queries.

Reference: https://rbi.org.in/Scripts/AnnualReportMainDisplay.aspx

Currently, two APIs are exposed, one for document ingestion and another for asking questions.

### 1. Document Upload API

POST `http://localhost:8080/api/documents/upload`

Here, the RBI Annual Report PDF has to be uploaded.

- **Key:** `file`
- **Value:** PDF file
- **Content-Type:** `multipart/form-data`

### 2. Ask Question API

GET `http://localhost:8080/api/rag/ask`

**Query Parameter:**
- **Key:** `question`
- **Value:** Question in text format

Example:

```http
http://localhost:8080/api/rag/ask?question=Why%20did%20the%20RBI%20reduce%20the%20CRR%20during%202025-26%2C%20and%20what%20was%20the%20final%20CRR%3F
```

### Sample API Response

```json
{
  "answer": "- The RBI reduced the CRR during 2025–26 to ease liquidity stress and improve system liquidity, as part of an easing cycle in monetary policy.\n- The final CRR was 3.0 per cent of net demand and time liabilities (NDTL).\n(Source 1, 3, 4, 5)",
  "sources": [
    {
      "sourceId": 1,
      "section": "II",
      "paragraphNumbers": ["4.2", "4.3"],
      "startPage": 31,
      "endPage": 32
    },
    {
      "sourceId": 2,
      "section": "II",
      "paragraphNumbers": ["4.3", "4.4", "4.5"],
      "startPage": 32,
      "endPage": 32
    },
    {
      "sourceId": 3,
      "section": "XII",
      "paragraphNumbers": ["4.4", "4.5"],
      "startPage": 183,
      "endPage": 184
    },
    {
      "sourceId": 4,
      "section": "III",
      "paragraphNumbers": ["1", "2", "3"],
      "startPage": 59,
      "endPage": 59
    },
    {
      "sourceId": 5,
      "section": "XII",
      "paragraphNumbers": ["23"],
      "startPage": 197,
      "endPage": 197
    }
  ]
}
```

### Response Structure

- `answer`: Generated answer based on the retrieved document content.
- `sources`: References used to generate the answer.
- `sourceId`: Unique ID of the source document.
- `section`: Section in which the relevant information is present.
- `paragraphNumbers`: Paragraph numbers containing the relevant information.
- `startPage`: Starting page number.
- `endPage`: Ending page number.


### Database
#### PostgreSQL + Vector extension (to store vector embeddings)