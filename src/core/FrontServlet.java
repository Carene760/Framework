// Dans src/core/FrontServlet.java
package core;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.MultipartConfig;

import core.model.ModelView;
import core.annotations.Param;
import core.utils.AnnotationScanner;
import core.utils.ParametersHandler;
import core.utils.UrlPattern;

// ====================================================
// SPRINT 10: Annotation pour support multipart
// ====================================================
@MultipartConfig(
    maxFileSize = 1024 * 1024 * 10,      // 10MB max par fichier
    maxRequestSize = 1024 * 1024 * 50,   // 50MB max par requête
    fileSizeThreshold = 1024 * 1024      // 1MB avant écriture disque
)
public class FrontServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        System.out.println("=== Initialisation du Framework ===");

        String pkg = getInitParameter("controller-package");
        System.out.println("Scan du package : " + pkg);

        AnnotationScanner.scanControllers(pkg);
        System.out.println("Routes trouvées : " + Router.routes.keySet());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    private void processRequest(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
    response.setContentType("text/html;charset=UTF-8");
    PrintWriter out = response.getWriter();

    String path = request.getRequestURI().substring(request.getContextPath().length());
    String requestMethod = request.getMethod();
    System.out.println("URL demandée: " + path + " [" + requestMethod + "]");

    try {
        Object routeInfo = Router.routes.get(path);
        
        if (routeInfo == null) {
            routeInfo = findMatchingPattern(path, request);
        }
        
        if (routeInfo != null && routeInfo instanceof Object[]) {
            Object[] route = (Object[]) routeInfo;
            Class<?> controllerClass = (Class<?>) route[0];
            Method method = (Method) route[1];
            String routeHttpMethod = (String) route[2];
            
            if (!routeHttpMethod.equals("ANY") && !routeHttpMethod.equals(requestMethod)) {
                out.println("<h2>Erreur 405 - Méthode non autorisée</h2>");
                out.println("<p>La route " + path + " nécessite la méthode: " + routeHttpMethod + "</p>");
                out.println("<p>Méthode reçue: " + requestMethod + "</p>");
                return;
            }
            
            Object controller = controllerClass.getDeclaredConstructor().newInstance();
            Object result = invokeMethodWithParameters(method, controller, request);

            // SPRINT 9: VÉRIFICATION SI LA MÉTHODE EST @Json
            if (method.isAnnotationPresent(core.annotations.Json.class)) {
                // SPRINT 9: Retourner du JSON
                handleJsonResponse(response, result);
                return;
            }
            
            // SPRINTS 1-8: CODE ORIGINAL (PAS TOUCHÉ)
            if (result instanceof String) {
                out.println((String) result); 
            } else if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;
                mv.getData().forEach(request::setAttribute);
                RequestDispatcher dispatcher = request.getRequestDispatcher(mv.getView());
                dispatcher.forward(request, response);
            } else {
                out.println("<h2>Résultat: " + result + "</h2>");
            }
        } else {
            out.println("<h2>Aucune route trouvée pour: " + path + "</h2>");
            out.println("<p>Routes disponibles: " + Router.routes.keySet() + "</p>");
        }
    } catch (Exception e) {
        out.println("<h2>Erreur: " + e.getMessage() + "</h2>");
        e.printStackTrace(out);
    }
}

    private Object findMatchingPattern(String path, HttpServletRequest request) {
        for (Map.Entry<String, Object> entry : Router.routes.entrySet()) {
            String routePattern = entry.getKey();
            Object routeInfo = entry.getValue();
            
            if (routePattern.contains("{") && routePattern.contains("}")) {
                UrlPattern urlPattern = new UrlPattern(routePattern);
                
                if (urlPattern.matches(path)) {
                    System.out.println("Pattern trouvé: " + routePattern + " → " + path);
                    
                    Map<String, String> urlParams = urlPattern.extractParams(path);
                    for (Map.Entry<String, String> param : urlParams.entrySet()) {
                        System.out.println("Paramètre URL extrait: " + param.getKey() + " = " + param.getValue());
                        request.setAttribute(param.getKey(), param.getValue());
                    }
                    
                    return routeInfo;
                }
            }
        }
        return null;
    }

    private Object invokeMethodWithParameters(Method method, Object controller, HttpServletRequest request)
        throws Exception {
        Class<?>[] paramTypes = method.getParameterTypes();
        java.lang.reflect.Parameter[] params = method.getParameters();

        if (paramTypes.length == 0) {
            return method.invoke(controller);
        }

        Object[] parameters = new Object[paramTypes.length];

        for (int i = 0; i < paramTypes.length; i++) {
            String paramName;
            
            if (params[i].isAnnotationPresent(Param.class)) {
                Param paramAnnotation = params[i].getAnnotation(Param.class);
                paramName = paramAnnotation.value();
            } else {
                paramName = params[i].getName();
            }
            
            // ====================================================
            // SPRINT 10: DÉTECTION DES FICHIERS UPLOADÉS
            // ====================================================
            if (params[i].isAnnotationPresent(core.annotations.FileUpload.class)) {
                core.annotations.FileUpload fileUploadAnn = params[i].getAnnotation(core.annotations.FileUpload.class);
                String fieldName = fileUploadAnn != null && !fileUploadAnn.value().isEmpty() 
                                  ? fileUploadAnn.value() 
                                  : paramName;
                
                System.out.println("SPRINT 10: Traitement upload fichier pour le champ: " + fieldName);
                
                if (paramTypes[i] == core.model.UploadedFile.class) {
                    parameters[i] = handleSingleFileUpload(request, fieldName);
                    continue;
                }
                
                if (paramTypes[i].isArray() && paramTypes[i].getComponentType() == core.model.UploadedFile.class) {
                    parameters[i] = handleMultipleFileUpload(request, fieldName);
                    continue;
                }
            }
            
            if (!isSimpleType(paramTypes[i]) && paramTypes[i] != HttpServletRequest.class 
                && paramTypes[i] != Map.class && !paramTypes[i].isArray()) {
                
                System.out.println("SPRINT 8 BIS: Construction d'objet complexe de type " + paramTypes[i].getName());
                parameters[i] = buildObjectFromRequest(paramName, paramTypes[i], request);
                continue;
            }
            
            if (paramTypes[i].isArray() && !isSimpleType(paramTypes[i].getComponentType())) {
                System.out.println("SPRINT 8 BIS: Construction de tableau d'objets de type " 
                    + paramTypes[i].getComponentType().getName());
                parameters[i] = buildArrayFromRequest(paramName, paramTypes[i].getComponentType(), request);
                continue;
            }
            
            String paramValue = request.getParameter(paramName);
            
            if (paramValue == null) {
                Object attrValue = request.getAttribute(paramName);
                if (attrValue != null) {
                    paramValue = attrValue.toString();
                    System.out.println("Paramètre URL injecté: " + paramName + " = " + paramValue);
                }
            }

            if (paramTypes[i] == HttpServletRequest.class) {
                parameters[i] = request;
            } else if (paramTypes[i] == Map.class) {
                Map<String, Object> dataMap = new HashMap<>();

                Map<String, String[]> paramMap = request.getParameterMap();
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    if (values.length == 1) {
                        dataMap.put(key, values[0]);
                    } else {
                        dataMap.put(key, values);
                    }
                }

                Enumeration<String> attrNames = request.getAttributeNames();
                while (attrNames.hasMoreElements()) {
                    String attrName = attrNames.nextElement();
                    Object attrValue = request.getAttribute(attrName);
                    dataMap.put(attrName, attrValue);
                }

                parameters[i] = dataMap;
            } else {
                parameters[i] = ParametersHandler.convertToType(paramValue, paramTypes[i]);
            }
        }

        return method.invoke(controller, parameters);
    }
    
    // ====================================================
    // SPRINT 10: MÉTHODES POUR L'UPLOAD DE FICHIERS
    // ====================================================
    
    /**
     * Gère l'upload d'un seul fichier
     */
    private core.model.UploadedFile handleSingleFileUpload(HttpServletRequest request, String fieldName) 
            throws IOException, ServletException {
        
        System.out.println("SPRINT 10: Upload fichier unique - champ: " + fieldName);
        
        if (!isMultipartRequest(request)) {
            System.out.println("SPRINT 10: Requête non multipart");
            return new core.model.UploadedFile();
        }
        
        Part filePart = request.getPart(fieldName);
        if (filePart == null || filePart.getSize() == 0) {
            System.out.println("SPRINT 10: Aucun fichier pour le champ " + fieldName);
            return new core.model.UploadedFile();
        }
        
        String fileName = getFileName(filePart);
        if (fileName == null || fileName.isEmpty()) {
            System.out.println("SPRINT 10: Nom de fichier vide");
            return new core.model.UploadedFile();
        }
        
        System.out.println("SPRINT 10: Fichier reçu: " + fileName + 
                         " (" + filePart.getSize() + " bytes, " + 
                         filePart.getContentType() + ")");
        
        core.model.UploadedFile uploadedFile = new core.model.UploadedFile();
        uploadedFile.setFileName(fileName);
        uploadedFile.setContentType(filePart.getContentType());
        uploadedFile.setSize(filePart.getSize());
        
        // Lire le contenu du fichier
        try (InputStream inputStream = filePart.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            uploadedFile.setContent(outputStream.toByteArray());
            System.out.println("SPRINT 10: Fichier chargé en mémoire: " + uploadedFile.getContent().length + " bytes");
            
        } catch (IOException e) {
            System.err.println("SPRINT 10: Erreur lecture fichier: " + e.getMessage());
        }
        
        return uploadedFile;
    }
    
    /**
     * Gère l'upload de plusieurs fichiers
     */
    private core.model.UploadedFile[] handleMultipleFileUpload(HttpServletRequest request, String fieldName) 
            throws IOException, ServletException {
        
        System.out.println("SPRINT 10: Upload multiple fichiers - champ: " + fieldName);
        
        if (!isMultipartRequest(request)) {
            System.out.println("SPRINT 10: Requête non multipart");
            return new core.model.UploadedFile[0];
        }
        
        Collection<Part> parts = request.getParts();
        List<core.model.UploadedFile> uploadedFiles = new ArrayList<>();
        
        if (parts != null) {
            for (Part part : parts) {
                if (part.getName() != null && part.getName().equals(fieldName) 
                    && part.getSize() > 0) {
                    
                    String fileName = getFileName(part);
                    if (fileName != null && !fileName.isEmpty()) {
                        System.out.println("SPRINT 10: Fichier multiple trouvé: " + fileName);
                        
                        core.model.UploadedFile uploadedFile = new core.model.UploadedFile();
                        uploadedFile.setFileName(fileName);
                        uploadedFile.setContentType(part.getContentType());
                        uploadedFile.setSize(part.getSize());
                        
                        // Lire le contenu
                        try (InputStream inputStream = part.getInputStream();
                             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                            
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = inputStream.read(buffer)) != -1) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                            
                            uploadedFile.setContent(outputStream.toByteArray());
                            uploadedFiles.add(uploadedFile);
                            
                        } catch (IOException e) {
                            System.err.println("SPRINT 10: Erreur lecture fichier multiple: " + e.getMessage());
                        }
                    }
                }
            }
        }
        
        System.out.println("SPRINT 10: " + uploadedFiles.size() + " fichiers uploadés");
        return uploadedFiles.toArray(new core.model.UploadedFile[0]);
    }
    
    /**
     * Vérifie si la requête est multipart (upload fichiers)
     */
    private boolean isMultipartRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/form-data");
    }
    
    /**
     * Extrait le nom de fichier d'une Part
     */
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String token : contentDisposition.split(";")) {
                if (token.trim().startsWith("filename")) {
                    String fileName = token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
                    // Extraire juste le nom du fichier (sans le chemin complet)
                    if (fileName.contains("\\")) {
                        fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
                    }
                    if (fileName.contains("/")) {
                        fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
                    }
                    return fileName;
                }
            }
        }
        return null;
    }
    
    private Object buildObjectFromRequest(String prefix, Class<?> type, HttpServletRequest request) 
            throws Exception {
        
        if (type.isArray()) {
            Class<?> compType = type.getComponentType();
            return buildArrayFromRequest(prefix, compType, request);
        }
        
        Object instance = type.getDeclaredConstructor().newInstance();
        Map<String, String[]> paramMap = request.getParameterMap();
        
        java.lang.reflect.Field[] fields = type.getDeclaredFields();
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            Class<?> fieldType = field.getType();
            String fullParamName = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
            
            if (isSimpleType(fieldType)) {
                String[] values = paramMap.get(fullParamName);
                if (values != null && values.length > 0) {
                    Object convertedValue = ParametersHandler.convertToType(values[0], fieldType);
                    field.set(instance, convertedValue);
                }
            } else if (fieldType.isArray()) {
                Object array = buildArrayFromRequest(fullParamName, fieldType.getComponentType(), request);
                field.set(instance, array);
            } else {
                boolean hasMatchingParams = false;
                for (String paramKey : paramMap.keySet()) {
                    if (paramKey.startsWith(fullParamName + ".")) {
                        hasMatchingParams = true;
                        break;
                    }
                }
                if (hasMatchingParams) {
                    Object nestedObject = buildObjectFromRequest(fullParamName, fieldType, request);
                    field.set(instance, nestedObject);
                }
            }
        }
        
        return instance;
    }
    
    private Object buildArrayFromRequest(String prefix, Class<?> componentType, HttpServletRequest request) 
            throws Exception {
        
        Map<String, String[]> paramMap = request.getParameterMap();
        java.util.Set<Integer> indices = new java.util.TreeSet<>();
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            java.util.regex.Pattern.quote(prefix) + "\\[(\\d+)\\](?:\\.|$)");
        
        for (String paramKey : paramMap.keySet()) {
            java.util.regex.Matcher matcher = pattern.matcher(paramKey);
            if (matcher.find()) {
                try {
                    int index = Integer.parseInt(matcher.group(1));
                    indices.add(index);
                } catch (NumberFormatException ignored) {}
            }
        }
        
        if (indices.isEmpty()) {
            String[] directValues = paramMap.get(prefix);
            if (directValues != null && isSimpleType(componentType)) {
                Object array = java.lang.reflect.Array.newInstance(componentType, directValues.length);
                for (int i = 0; i < directValues.length; i++) {
                    Object convertedValue = ParametersHandler.convertToType(directValues[i], componentType);
                    java.lang.reflect.Array.set(array, i, convertedValue);
                }
                return array;
            }
            return java.lang.reflect.Array.newInstance(componentType, 0);
        }
        
        int maxIndex = indices.stream().max(Integer::compareTo).orElse(-1);
        Object array = java.lang.reflect.Array.newInstance(componentType, maxIndex + 1);
        
        for (int index : indices) {
            String elementPrefix = prefix + "[" + index + "]";
            
            if (isSimpleType(componentType)) {
                String[] values = paramMap.get(elementPrefix);
                if (values != null && values.length > 0) {
                    Object convertedValue = ParametersHandler.convertToType(values[0], componentType);
                    java.lang.reflect.Array.set(array, index, convertedValue);
                }
            } else {
                Object element = buildObjectFromRequest(elementPrefix, componentType, request);
                java.lang.reflect.Array.set(array, index, element);
            }
        }
        
        return array;
    }
    
    private boolean isSimpleType(Class<?> type) {
        if (type.isPrimitive()) return true;
        if (type == String.class) return true;
        if (Number.class.isAssignableFrom(type)) return true;
        if (type == Boolean.class || type == Character.class) return true;
        if (java.util.Date.class.isAssignableFrom(type)) return true;
        if (type.isEnum()) return true;
        return false;
    }

    private String getFirstParameterName(HttpServletRequest request) {
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            return params.keySet().iterator().next();
        }
        return null;
    }

    // SPRINT 9: MÉTHODES POUR LE JSON
    private void handleJsonResponse(HttpServletResponse response, Object result) throws IOException {
        try {
            if (result instanceof ModelView) {
                ModelView mv = (ModelView) result;
                writeJsonSuccess(response, 200, mv.getData());
            } else {
                writeJsonSuccess(response, 200, result);
            }
        } catch (Exception e) {
            writeJsonError(response, 500, "Erreur lors de la génération JSON: " + e.getMessage());
        }
    }

    private void writeJsonSuccess(HttpServletResponse response, int code, Object data) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        
        try (PrintWriter out = response.getWriter()) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"success\",");
            json.append("\"code\":").append(code).append(",");
            
            if (data != null && (data.getClass().isArray() || data instanceof Collection)) {
                int count = 0;
                if (data.getClass().isArray()) {
                    count = java.lang.reflect.Array.getLength(data);
                } else {
                    count = ((Collection<?>) data).size();
                }
                json.append("\"count\":").append(count).append(",");
            }
            
            json.append("\"data\":");
            json.append(convertToJson(data));
            json.append("}");
            
            out.println(json.toString());
        }
    }

    private void writeJsonError(HttpServletResponse response, int code, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(code);
        
        try (PrintWriter out = response.getWriter()) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"status\":\"error\",");
            json.append("\"code\":").append(code).append(",");
            json.append("\"data\":{");
            json.append("\"message\":\"").append(escapeJson(message)).append("\"");
            json.append("}");
            json.append("}");
            
            out.println(json.toString());
        }
    }

    private String convertToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        
        if (obj instanceof String) {
            return "\"" + escapeJson((String) obj) + "\"";
        }
        
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        
        if (obj.getClass().isArray()) {
            StringBuilder json = new StringBuilder();
            json.append("[");
            
            int length = java.lang.reflect.Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                Object element = java.lang.reflect.Array.get(obj, i);
                if (i > 0) {
                    json.append(",");
                }
                json.append(convertToJson(element));
            }
            
            json.append("]");
            return json.toString();
        }
        
        if (obj instanceof Collection) {
            StringBuilder json = new StringBuilder();
            json.append("[");
            
            Collection<?> collection = (Collection<?>) obj;
            int i = 0;
            for (Object element : collection) {
                if (i > 0) {
                    json.append(",");
                }
                json.append(convertToJson(element));
                i++;
            }
            
            json.append("]");
            return json.toString();
        }
        
        if (obj instanceof Map) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            
            Map<?, ?> map = (Map<?, ?>) obj;
            int i = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(escapeJson(entry.getKey().toString())).append("\":");
                json.append(convertToJson(entry.getValue()));
                i++;
            }
            
            json.append("}");
            return json.toString();
        }
        
        return convertObjectToJson(obj);
    }

    private String convertObjectToJson(Object obj) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        java.lang.reflect.Field[] fields = obj.getClass().getDeclaredFields();
        int i = 0;
        for (java.lang.reflect.Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(field.getName()).append("\":");
                json.append(convertToJson(value));
                i++;
            } catch (IllegalAccessException e) {
                // Ignorer les champs inaccessibles
            }
        }
        
        json.append("}");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}