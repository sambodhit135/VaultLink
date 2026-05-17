package com.vaultlink.service;

import com.vaultlink.dto.request.DocumentRequest;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.DocumentResponse;
import com.vaultlink.dto.response.ExpiryDashboardResponse;

import java.util.List;

public interface DocumentService {

    List<DocumentResponse> getAllDocuments(String email);

    DocumentResponse getDocumentById(Long id, String email);

    DocumentResponse createDocument(DocumentRequest request, String email);

    DocumentResponse updateDocument(Long id, DocumentRequest request, String email);

    ApiResponse deleteDocument(Long id, String email);

    List<DocumentResponse> getDocumentsByCategory(Long categoryId, String email);

    List<DocumentResponse> getDocumentsByStatus(String status, String email);

    List<DocumentResponse> getExpiringSoon(String email, int days);

    ExpiryDashboardResponse getExpiryDashboard(String email);
}
