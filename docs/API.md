# 🔌 API Endpoints Documentation

This document lists all the available REST API endpoints for **VaultLink**. All endpoints, except registration and login, require a valid JWT token passed in the `Authorization` header as `Bearer <token>`.

## 🛡️ Authentication Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/register` | Register a new user account | ❌ No |
| `POST` | `/api/auth/login` | Log in and return a JWT access token | ❌ No |
| `PUT` | `/api/auth/change-password` | Update current user password | ✅ Yes |

---

## 📁 Document Management Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/documents` | Retrieve all documents owned by the user | ✅ Yes |
| `POST` | `/api/documents` | Add a new document record | ✅ Yes |
| `GET` | `/api/documents/{id}` | Fetch detailed document record by ID | ✅ Yes |
| `PUT` | `/api/documents/{id}` | Update an existing document record | ✅ Yes |
| `DELETE` | `/api/documents/{id}` | Soft delete a document record | ✅ Yes |
| `GET` | `/api/documents/expiry/summary` | Get Expiry dashboard urgency tier metrics | ✅ Yes |
| `GET` | `/api/documents/expiring-soon` | Retrieve documents expiring in N days | ✅ Yes |
| `GET` | `/api/documents/status/{status}` | Filter documents by compliance status | ✅ Yes |
| `GET` | `/api/documents/category/{id}` | Filter documents by category ID | ✅ Yes |
| `GET` | `/api/documents/search` | Search documents by name/keywords | ✅ Yes |

---

## 🔗 Secure Sharing Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/documents/{id}/share` | Generate a secure, expiring share link | ✅ Yes |
| `GET` | `/api/documents/shared/{token}` | Access a shared document via its token | ❌ No |

---

## 🏷️ Category Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/categories` | Retrieve all available document categories | ❌ No |
| `POST` | `/api/categories` | Create a new custom document category | ✅ Yes |

---

## 🔔 Notification Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/notifications` | View dispatched notification logs history | ✅ Yes |
| `POST` | `/api/notifications/trigger-check`| Trigger manual compliance expiry check | ✅ Yes |

---

## ⚡ Cache Endpoints

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `GET` | `/api/cache/stats` | View real-time Redis cache size/hit stats | ✅ Yes |
