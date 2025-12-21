package core.utils;

import java.lang.reflect.Method;

public class MappingHandler {
    private Class<?> classe;
    private Method methode;
    private String httpMethod;

    // ====================================================
    // CONSTRUCTEUR COMPLET: Classe + Méthode + HTTP Method
    // ====================================================
    public MappingHandler(Class<?> classe, Method methode, String httpMethod) {
        this.classe = classe;
        this.methode = methode;
        this.httpMethod = httpMethod;
    }

    // ====================================================
    // CONSTRUCTEUR: Méthode + HTTP Method seulement
    // ====================================================
    public MappingHandler(Method methode, String httpMethod) {
        this.methode = methode;
        this.httpMethod = httpMethod;
    }

    // ====================================================
    // CONSTRUCTEUR: Classe + Méthode seulement (HTTP Method = null)
    // ====================================================
    public MappingHandler(Class<?> classe, Method methode) {
        this.classe = classe;
        this.methode = methode;
        this.httpMethod = "ANY"; // Valeur par défaut
    }

    // ====================================================
    // CONSTRUCTEUR VIDE
    // ====================================================
    public MappingHandler() {
    }

    // ====================================================
    // GETTERS ET SETTERS
    // ====================================================
    public Class<?> getClasse() {
        return classe;
    }

    public void setClasse(Class<?> classe) {
        this.classe = classe;
    }

    public Method getMethode() {
        return methode;
    }

    public void setMethode(Method methode) {
        this.methode = methode;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
}