package com.froidcheikh.ecommerce.dto;


import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {
    private boolean success;
    private String message;
    private List<String> uploadedFiles;
    private List<String> errors;

    public static FileUploadResponse success(List<String> files) {
        return new FileUploadResponse(true, "Fichiers uploadés avec succès", files, null);
    }

    public static FileUploadResponse success(String file) {
        return new FileUploadResponse(true, "Fichier uploadé avec succès", List.of(file), null);
    }

    public static FileUploadResponse error(List<String> errors) {
        return new FileUploadResponse(false, "Erreur lors de l'upload", null, errors);
    }

    public static FileUploadResponse error(String error) {
        return new FileUploadResponse(false, "Erreur lors de l'upload", null, List.of(error));
    }
}