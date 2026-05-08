




Design a campus notification platform for students receiving Placement, Event, and Result notifications, with a real-time delivery mechanism.



I would use SSE for the real-time channel.

Why SSE:
- The use case is one-way delivery from server to student client.
- It works well for browser-based dashboards and mobile web clients.
- It is simpler than WebSockets for notification push because the server only needs to stream events.
- It automatically supports reconnect behavior with `Last-Event-ID`.
- It keeps the API aligned with HTTP and is easier to operate behind proxies than a custom socket protocol.

WebSockets would be justified only if the client also needed bidirectional presence or chat-like interaction. Long polling is less efficient and adds unnecessary request churn.




- Method: `POST`
- URL: `/api/v1/notifications`
- Purpose: Create a notification for one or more students.
- Request Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`

- Request Body Schema:
```json
{
  "type": "Placement | Event | Result",
  "title": "string",
  "message": "string",
  "recipientStudentIds": ["string"],
  "scheduledAt": "string | null"
}
```

- Response Body Schema:
```json
{
  "notificationId": "string",
  "type": "Placement | Event | Result",
  "title": "string",
  "message": "string",
  "recipientCount": 0,
  "status": "CREATED | SCHEDULED | SENT"
}
```

- Status Codes:
  - `201 Created`
  - `400 Bad Request`
  - `401 Unauthorized`
  - `403 Forbidden`
  - `500 Internal Server Error`


- Method: `GET`
- URL: `/api/v1/students/{studentId}/notifications`
- Purpose: Fetch paginated notifications for a student.
- Request Headers:
  - `Authorization: Bearer <token>`
  - `Accept: application/json`

- Request Body: None

- Response Body Schema:
```json
{
  "studentId": "string",
  "items": [
    {
      "notificationId": "string",
      "type": "Placement | Event | Result",
      "title": "string",
      "message": "string",
      "isRead": false,
      "createdAt": "string"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 0
}
```

- Status Codes:
  - `200 OK`
  - `401 Unauthorized`
  - `404 Not Found`


- Method: `GET`
- URL: `/api/v1/students/{studentId}/notifications/unread`
- Purpose: Return only unread notifications.
- Request Headers:
  - `Authorization: Bearer <token>`

- Request Body: None

- Response Body Schema:
```json
{
  "studentId": "string",
  "unreadCount": 0,
  "items": [
    {
      "notificationId": "string",
      "type": "Placement | Event | Result",
      "message": "string",
      "createdAt": "string"
    }
  ]
}
```

- Status Codes:
  - `200 OK`
  - `401 Unauthorized`
  - `404 Not Found`


- Method: `PATCH`
- URL: `/api/v1/students/{studentId}/notifications/{notificationId}/read`
- Purpose: Mark a notification as read.
- Request Headers:
  - `Authorization: Bearer <token>`
  - `Content-Type: application/json`

- Request Body: None

- Response Body Schema:
```json
{
  "notificationId": "string",
  "studentId": "string",
  "isRead": true,
  "readAt": "string"
}
```

- Status Codes:
  - `200 OK`
  - `400 Bad Request`
  - `401 Unauthorized`
  - `404 Not Found`


- Method: `GET`
- URL: `/api/v1/notifications/stream`
- Purpose: Open an SSE stream for live delivery.
- Request Headers:
  - `Authorization: Bearer <token>`
  - `Accept: text/event-stream`

- Request Body: None

- Response Body Schema:
```json
{
  "eventId": "string",
  "studentId": "string",
  "type": "Placement | Event | Result",
  "title": "string",
  "message": "string",
  "createdAt": "string"
}
```

- Status Codes:
  - `200 OK`
  - `401 Unauthorized`
  - `403 Forbidden`


- Notification creation should support both one-to-one and broadcast delivery.
- Read state must be tracked per student, not globally per notification.
- Client pagination should use cursor-based pagination at scale instead of offset pagination.




Use PostgreSQL.

Why PostgreSQL:
- Strong relational structure fits students, notifications, recipients, and read state.
- Supports transactions for consistent notification creation and delivery metadata.
- Has strong indexing and partitioning features.
- Handles analytical queries well for reporting and retention jobs.
- Enforces constraints cleanly for data integrity.




- `student_id` UUID PRIMARY KEY
- `roll_no` VARCHAR(50) NOT NULL UNIQUE
- `name` VARCHAR(150) NOT NULL
- `email` VARCHAR(255) NOT NULL UNIQUE
- `department` VARCHAR(100) NOT NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP

Constraints:
- Unique `roll_no`
- Unique `email`
- Non-null identity and contact fields


- `notification_id` UUID PRIMARY KEY
- `notification_type` VARCHAR(20) NOT NULL CHECK (notification_type IN ('Placement', 'Event', 'Result'))
- `title` VARCHAR(200) NOT NULL
- `message` TEXT NOT NULL
- `created_by` UUID NULL
- `status` VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'SCHEDULED', 'SENT', 'FAILED'))
- `scheduled_at` TIMESTAMP NULL
- `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
- `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP


- `notification_id` UUID NOT NULL REFERENCES notifications(notification_id) ON DELETE CASCADE
- `student_id` UUID NOT NULL REFERENCES students(student_id) ON DELETE CASCADE
- `is_read` BOOLEAN NOT NULL DEFAULT FALSE
- `delivered_at` TIMESTAMP NULL
- `read_at` TIMESTAMP NULL
- `delivery_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (delivery_status IN ('PENDING', 'DELIVERED', 'FAILED'))
- PRIMARY KEY (`notification_id`, `student_id`)

Indexes:
- `idx_notification_recipients_student_read_created` on (`student_id`, `is_read`, `delivered_at` DESC)
- `idx_notification_recipients_student_created` on (`student_id`, `delivered_at` DESC)
- `idx_notifications_type_created` on (`notification_type`, `created_at` DESC)


- `student_id` UUID NOT NULL REFERENCES students(student_id) ON DELETE CASCADE
- `notification_type` VARCHAR(20) NOT NULL CHECK (notification_type IN ('Placement', 'Event', 'Result'))
- `enabled` BOOLEAN NOT NULL DEFAULT TRUE
- PRIMARY KEY (`student_id`, `notification_type`)




```sql
BEGIN;

INSERT INTO notifications (
  notification_id, notification_type, title, message, created_by, status, scheduled_at
) VALUES (
  :notificationId, :type, :title, :message, :createdBy, :status, :scheduledAt
);

INSERT INTO notification_recipients (
  notification_id, student_id, is_read, delivered_at, read_at, delivery_status
)
SELECT
  :notificationId,
  s.student_id,
  FALSE,
  NULL,
  NULL,
  'PENDING'
FROM students s
WHERE s.student_id = ANY(:recipientStudentIds);

COMMIT;
```


```sql
SELECT
  nr.notification_id,
  n.notification_type,
  n.title,
  n.message,
  nr.is_read,
  COALESCE(nr.delivered_at, n.created_at) AS created_at
FROM notification_recipients nr
JOIN notifications n ON n.notification_id = nr.notification_id
WHERE nr.student_id = :studentId
ORDER BY COALESCE(nr.delivered_at, n.created_at) DESC
LIMIT :limit OFFSET :offset;
```


```sql
SELECT
  nr.notification_id,
  n.notification_type,
  n.title,
  n.message,
  COALESCE(nr.delivered_at, n.created_at) AS created_at
FROM notification_recipients nr
JOIN notifications n ON n.notification_id = nr.notification_id
WHERE nr.student_id = :studentId
  AND nr.is_read = FALSE
ORDER BY COALESCE(nr.delivered_at, n.created_at) DESC;
```


```sql
UPDATE notification_recipients
SET is_read = TRUE,
    read_at = CURRENT_TIMESTAMP,
    delivery_status = 'DELIVERED'
WHERE student_id = :studentId
  AND notification_id = :notificationId;
```


The SSE stream is backed by the same relational data, but the stream itself is driven by an event publisher. When a client reconnects, the server can query missed notifications:
```sql
SELECT
  nr.notification_id,
  n.notification_type,
  n.title,
  n.message,
  COALESCE(nr.delivered_at, n.created_at) AS created_at
FROM notification_recipients nr
JOIN notifications n ON n.notification_id = nr.notification_id
WHERE nr.student_id = :studentId
  AND COALESCE(nr.delivered_at, n.created_at) > :lastEventTime
ORDER BY COALESCE(nr.delivered_at, n.created_at) ASC;
```



- Notification fan-out creates many writes per campaign.
- Unread inbox queries become slow without composite indexes.
- Offset pagination gets slower as page number grows.
- Real-time delivery can overwhelm one application instance.
- Storage growth becomes significant if old notifications are never archived.
- Counting unread notifications repeatedly can become expensive.



- Use queue-based asynchronous fan-out for bulk sends.
- Create composite indexes that match the query pattern.
- Use cursor-based pagination instead of offset pagination.
- Partition notification tables by time range or hash of student_id if needed.
- Cache unread counts and hot inbox data in Redis.
- Archive old notifications into cold storage tables.
- Use read replicas for read-heavy traffic.
- Use idempotent consumers for safe retries.




```sql
SELECT * FROM notifications
WHERE studentID = 1042 AND isRead = false
ORDER BY createdAt DESC;
```


- It is syntactically valid in SQL, assuming the column names match the table definition.
- The main issues are that it uses `SELECT *`, which reads unnecessary columns, and it does not specify a `LIMIT` if only the newest unread notifications are needed.
- If the schema uses snake_case, the column names may actually be `student_id`, `is_read`, and `created_at`, so the query could be inconsistent with the real schema.


- Without a suitable composite index, the database must scan many rows to find matching `studentID` and `isRead` values.
- The `ORDER BY createdAt DESC` forces a sort unless the index already supports the ordering.
- On a table with millions of rows, a full scan plus sort is expensive.


- Add a composite index such as:
```sql
CREATE INDEX idx_notifications_student_read_created
ON notifications (student_id, is_read, created_at DESC);
```
- Rewrite the query to select only needed columns:
```sql
SELECT notification_id, notification_type, message, created_at
FROM notifications
WHERE student_id = 1042 AND is_read = FALSE
ORDER BY created_at DESC;
```
- If the UI only needs a page of results, add `LIMIT` and cursor-based paging.
- Likely cost improves from a table scan and filesort to an indexed lookup with a small range scan, roughly from $O(n \log n)$ behavior to about $O(\log n + k)$ for the matching rows.
- The index creation itself costs about $O(n \log n)$ once, plus extra storage and write overhead.


No.
- Every index increases write cost because inserts, updates, and deletes must maintain more structures.
- Indexes consume memory and disk.
- Too many indexes can confuse the optimizer and slow writes without improving the actual query patterns.
- The better approach is a small set of selective composite indexes based on real access patterns.


```sql
SELECT DISTINCT student_id
FROM notifications
WHERE notification_type = 'Placement'
  AND created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days';
```

If the application needs student details as well, join the students table:
```sql
SELECT DISTINCT s.student_id, s.name, s.email
FROM notifications n
JOIN students s ON s.student_id = n.student_id
WHERE n.notification_type = 'Placement'
  AND n.created_at >= CURRENT_TIMESTAMP - INTERVAL '7 days';
```




Use Redis with a cache-aside pattern.

Why Redis:
- Notifications are read frequently and updated relatively less often.
- Redis is shared across instances, unlike in-memory caches.
- It supports fast lookup for unread counts and inbox slices.
- TTL-based expiration and explicit invalidation are easy to manage.


- Cache the unread inbox per student using a key like `student:{id}:notifications:unread`.
- Cache unread counts separately using `student:{id}:notifications:unread-count`.
- When a new notification is created for a student, invalidate or update that studentâ€™s cache entries.
- When a student marks a notification as read, evict or update the relevant cache keys.
- Use short TTLs for inbox payloads and slightly longer TTLs for counts.
- Keep the database as the source of truth and Redis as a read accelerator.




- Pros: fast, shared, good for high read volume, simple to invalidate by key.
- Cons: cache staleness if invalidation is missed, additional infrastructure, memory cost.


- Pros: very fast and simple for a single instance.
- Cons: not shared across app nodes, poor fit for horizontal scaling, cache inconsistency between instances.


- Pros: excellent for static assets.
- Cons: not suitable for per-student personalized notification data because the content is private and dynamic.


Use Redis as the main cache, optionally with a small local in-memory cache for ultra-hot lookups inside a single node. The shared cache should remain the primary layer.




The current implementation is fragile and slow.
- It performs email, database insert, and push sequentially in a single loop.
- One slow external call delays the whole batch.
- There is no retry policy or dead-letter handling.
- Partial failure causes inconsistent state across students.
- It does not batch DB writes or use queue-based processing.
- It does not support idempotency, so retries can duplicate work.


If `send_email` fails for 200 students midway:
- Some earlier students will already have been emailed, saved, and pushed.
- The loop will either stop or continue depending on the implementation, but either way the batch becomes partially complete.
- Recovery is difficult because the system does not know which side effects succeeded unless each step is tracked separately.

Recovery approach:
- Persist per-recipient delivery status.
- Retry failed email sends with backoff.
- Move unrecoverable failures into a dead-letter queue for manual review.
- Make every recipient processing step idempotent so retries are safe.


Use asynchronous batch processing with an outbox pattern.
- The HR action creates one bulk notification job.
- The job writes notification metadata and recipient rows inside the database transaction.
- A background worker reads pending recipients in batches.
- The worker sends email and push notifications separately with retries.
- Each delivery attempt is idempotent.
- Success and failure are tracked per recipient.
- The frontend can poll job status or receive a completion event.

This approach is faster because the HTTP request returns quickly and the heavy work runs asynchronously.


No.
- Email is an external side effect and cannot be made truly atomic with a database transaction.
- Trying to force atomicity across the DB and an email provider creates distributed transaction complexity without real reliability.
- The better design is eventual consistency with an outbox table or message queue.
- The DB transaction should commit the intended work, and the delivery workers should execute and retry the side effects.


```text
function notify_all(student_ids, message):
    job_id = create_bulk_notification_job(status = "PENDING")

    begin transaction
        notification_id = insert_notification(message, job_id)
        for student_id in student_ids:
            insert_notification_recipient(notification_id, student_id, delivery_status = "PENDING")
        insert_outbox_event(job_id, notification_id)
    commit transaction

    enqueue_job(job_id)
    return job_id

worker process_bulk_notification_job(job_id):
    recipients = fetch_pending_recipients(job_id, batch_size = 500)

    for recipient in recipients:
        try:
            send_email(recipient.student_id, recipient.message)
            push_to_app(recipient.student_id, recipient.message)
            mark_recipient_delivered(recipient)
        catch transient_error:
            increment_retry_count(recipient)
            requeue(recipient)
        catch permanent_error:
            mark_recipient_failed(recipient)
            send_to_dead_letter_queue(recipient)

    if all recipients processed:
        mark_job_completed(job_id)
```




The priority score is:
- Placement = 3
- Result = 2
- Event = 1

Sorting rule:
- Higher priority score first.
- For equal scores, newer timestamp first.


The implementation uses a min-heap of size 10.
- Each incoming notification is converted to a priority DTO.
- If the heap has fewer than 10 entries, the item is added.
- Once the heap is full, the new item is compared with the weakest item at the top of the heap.
- If the new item is better, the weakest item is removed and the new one is inserted.
- This keeps memory bounded and ensures each insertion is $O(\log 10)$, which is effectively constant.


I chose `PriorityQueue`.
- It is the right fit for maintaining a bounded top-N list.
- It is faster than sorting the full list every time new notifications arrive.
- It keeps the implementation simple and deterministic.


The Stage 6 service fetches notifications from the protected evaluation API, scores them in memory, and returns the top 10 notifications in descending priority order.


```json
{
  "topNotifications": [
    {
      "id": "string",
      "type": "Placement",
      "message": "string",
      "timestamp": "string",
      "priorityScore": 3
    }
  ]
}
```

