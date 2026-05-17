package com.vaultlink.service;

import com.vaultlink.dto.request.ShareDocumentRequest;
import com.vaultlink.dto.response.AccessTokenResponse;
import com.vaultlink.dto.response.ApiResponse;
import com.vaultlink.dto.response.DocumentResponse;

import java.util.List;

public interface AccessTokenService {

    AccessTokenResponse shareDocument(Long documentId, ShareDocumentRequest request, String ownerEmail);

    DocumentResponse getSharedDocument(String token);

    List<AccessTokenResponse> getShareHistory(Long documentId, String ownerEmail);

    ApiResponse revokeToken(Long tokenId, String ownerEmail);
}
