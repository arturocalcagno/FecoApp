package com.arturocalcagno.fecoapp;

import android.annotation.SuppressLint;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import android.util.Log;

public class WebService {
    public final String NAMESPACE = "https://fecoapp.com.ar/";
    public final String URL = "https://www.fecoapp.com.ar/WebService/FecoAppWS.asmx";

    public String validarversion(String version) {
        final String SOAP_ACTION = "https://fecoapp.com.ar/ValidarVersion";
        final String METHOD_NAME = "ValidarVersion";

        SoapObject client;                       // It's the client petition to the web service
        SoapSerializationEnvelope sse;

        // Lo siguiente es para evitar validación de certificado SSL
        // Configuración del TrustManager para manejar certificados SSL
        @SuppressLint("CustomX509TrustManager") TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @SuppressLint("TrustAllX509TrustManager")
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            @SuppressLint("TrustAllX509TrustManager")
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        }};

        try {
            // Configuración del TrustManager personalizado
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            // Configuración de la conexión HTTPS con el TrustManager personalizado
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
        }

        sse = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        sse.dotNet = true; // Se establece que el servicio web está hecho en .net
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);

        client = new SoapObject(NAMESPACE, METHOD_NAME);
        client.addProperty("version", version);
        sse.setOutputSoapObject(client);

        try {
            // Llamada al servicio web
            androidHttpTransport.call(SOAP_ACTION, sse);
            // La respuesta del servicio web es un SoapPrimitive, no un SoapObject
            SoapPrimitive response = (SoapPrimitive) sse.getResponse();
            return response.toString();  // Devuelve la versión o "False"
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
            // En caso de excepción, devolver "False"
            return "False";
        }
    }

    public String registrarremito(String carga, String boca, String remito, String razonsocial, String registro, String fotoremito, String latitud, String longitud, String fotoremito2) {
        String res;
        String METHOD_NAME = "RegistrarRemitoNew2";
        String SOAP_ACTION = "https://fecoapp.com.ar/RegistrarRemitoNew2";

        // Lo siguiente es para evitar validación de certificado SSL
        // Configuración del TrustManager para manejar certificados SSL
        @SuppressLint("CustomX509TrustManager") TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            @SuppressLint("TrustAllX509TrustManager")
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }
            @SuppressLint("TrustAllX509TrustManager")
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[]{};
            }
        }};

        try {
            // Configuración del TrustManager personalizado
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            // Configuración de la conexión HTTPS con el TrustManager personalizado
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Crear la solicitud SOAP
        //Se crea un objeto de tipo SoapObjeto. Permite hacer el llamado al WS
        SoapObject client;
        client = new SoapObject(NAMESPACE, METHOD_NAME);
        client.addProperty("carga", carga);
        client.addProperty("boca", boca);
        client.addProperty("remito", remito);
        client.addProperty("razonsocial", razonsocial);
        client.addProperty("registro", registro);
        client.addProperty("fotoremito", fotoremito);
        client.addProperty("latitud", latitud);
        client.addProperty("longitud", longitud);
        client.addProperty("fotoremito2", fotoremito2);

        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        //envelope.bodyOut = rpc;
        //Se establece que el servicio web está hecho en .net
        envelope.dotNet = true;
        envelope.setOutputSoapObject(client);
        //envelope.encodingStyle = SoapSerializationEnvelope.XSD;
        //Para acceder al WS se crea un objeto de tipo HttpTransportSE, esto es propio de la librería KSoap
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
        try {
            //Llamado al servicio web. Son el nombre del SoapAction, que se encuentra en la documentación del servicio web y el objeto envelope
            androidHttpTransport.call(SOAP_ACTION, envelope);
            //Respuesta del Servicio web
            res = envelope.getResponse().toString();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            res = "false";
        }
        return res;
    }

    public String registrarcarga(String empresa, String celular, String region, String disponibilidad, String vehiculos) {
        String METHOD_NAME = "RegistrarCarga";
        String SOAP_ACTION = "https://fecoapp.com.ar/RegistrarCarga";

        String res = "false";  // Respuesta predeterminada en caso de error

        // Lo siguiente es para evitar validación de certificado SSL
        @SuppressLint("CustomX509TrustManager") TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }
                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };

        try {
            // Configuración del TrustManager personalizado para SSL
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            // Configuración de la conexión HTTPS con el TrustManager personalizado
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
            return res;  // Si falla la configuración SSL, devolvemos "false"
        }

        // Creación del objeto SoapObject para realizar la llamada al servicio web
        SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
        rpc.addProperty("empresa", empresa);
        rpc.addProperty("celular", celular);
        rpc.addProperty("region", region);
        rpc.addProperty("disponibilidad", disponibilidad);
        rpc.addProperty("vehiculos", vehiculos);

        // Configuración de la envoltura para SOAP
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = true;  // El servicio está basado en .NET
        envelope.setOutputSoapObject(rpc);

        // Transporte HTTP para la solicitud SOAP
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL, 60000);  // Timeout de 60 segundos

        try {
            // Llamada al servicio web
            androidHttpTransport.call(SOAP_ACTION, envelope);
            // Procesar la respuesta
            res = envelope.getResponse().toString();
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
            res = "false";  // En caso de error en la llamada, se retorna "false"
        }
        return res;
    }

    public String registrarcarganew(String empresa, String celular, String region, String disponibilidad, String vehiculos, String latitud, String longitud) {
        String METHOD_NAME = "RegistrarCargaNew";
        String SOAP_ACTION = "https://fecoapp.com.ar/RegistrarCargaNew";

        String res = "false";  // Respuesta predeterminada en caso de error

        // Lo siguiente es para evitar validación de certificado SSL
        @SuppressLint("CustomX509TrustManager") TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    }
                    @SuppressLint("TrustAllX509TrustManager")
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {
                    }
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[]{};
                    }
                }
        };

        try {
            // Configuración del TrustManager personalizado para SSL
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());

            // Configuración de la conexión HTTPS con el TrustManager personalizado
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
            return res;  // Si falla la configuración SSL, devolvemos "false"
        }

        // Creación del objeto SoapObject para realizar la llamada al servicio web
        SoapObject rpc = new SoapObject(NAMESPACE, METHOD_NAME);
        rpc.addProperty("empresa", empresa);
        rpc.addProperty("celular", celular);
        rpc.addProperty("region", region);
        rpc.addProperty("disponibilidad", disponibilidad);
        rpc.addProperty("vehiculos", vehiculos);
        rpc.addProperty("latitud", latitud);
        rpc.addProperty("longitud", longitud);

        // Configuración de la envoltura para SOAP
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
        envelope.dotNet = true;  // El servicio está basado en .NET
        envelope.setOutputSoapObject(rpc);

        // Transporte HTTP para la solicitud SOAP
        HttpTransportSE androidHttpTransport = new HttpTransportSE(URL, 60000);  // Timeout de 60 segundos

        try {
            // Llamada al servicio web
            androidHttpTransport.call(SOAP_ACTION, envelope);
            // Procesar la respuesta
            res = envelope.getResponse().toString();
        } catch (Exception e) {
            Log.e("DB_ERROR", "Error al agregar remito: ", e);  // Usar Log para registrar el error
            res = "false";  // En caso de error en la llamada, se retorna "false"
        }
        return res;
    }
}

