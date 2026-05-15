package com.epiis.projectcasaketteler.business;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epiis.projectcasaketteler.dto.request.RequestAttendanceInsert;
import com.epiis.projectcasaketteler.dto.response.ResponseFaceVerification;
import com.epiis.projectcasaketteler.entity.EntityAttendance;
import com.epiis.projectcasaketteler.entity.EntityUser;
import com.epiis.projectcasaketteler.helper.ObtainIpAddressHelper;
import com.epiis.projectcasaketteler.helper.PythonFaceRecognitionHelper;
import com.epiis.projectcasaketteler.repository.RepositoryAttendance;
import com.epiis.projectcasaketteler.repository.RepositoryUser;

@Service
public class BusinessAttendance {

    @Autowired
    RepositoryAttendance repositoryAttendance;
    
    @Autowired
    RepositoryUser repositoryUser;
    
    @Autowired
    PythonFaceRecognitionHelper pythonFaceRecognitionHelper;
    
    @Autowired
    ObtainIpAddressHelper obtainIpAddressHelper;
    
    public ResponseFaceVerification insert(RequestAttendanceInsert request) {
        File tempFile = null;
        ResponseFaceVerification response = new ResponseFaceVerification();
        
        try {
            String tempDir = "temp/";
            File directory = new File(tempDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            
            String fileName = "captura_" + request.getIdUser() + ".jpg";
            String rutaImagen = tempDir + fileName;
            tempFile = new File(rutaImagen);
            request.getFile().transferTo(tempFile);
            
            ResponseFaceVerification responsetoPython = pythonFaceRecognitionHelper.verificarRostro(rutaImagen, request.getIdUser());
            if (!responsetoPython.isVerified()) {
                responsetoPython.error();
                responsetoPython.getListMessage().add("Error: Algo salió mal con el reconocimiento facial. Vuelve a intentarlo.");
                return responsetoPython;
            }
            
            Optional<EntityUser> optionalUser = repositoryUser.findById(request.getIdUser());
            if (!optionalUser.isPresent()) {
                response.error();
                response.getListMessage().add("Error: Usuario no encontrado.");
                return response;
            }
            EntityUser entityUser = optionalUser.get();
            
            String localIpHost = InetAddress.getLocalHost().getHostAddress(); 
            String parentResidenceIp = entityUser.getParentResidence().getIpAddress();
            String requestIp = obtainIpAddressHelper.getIp();
            String userLocalAddress = entityUser.getIdAddressLocal();

            if (!localIpHost.equals(parentResidenceIp)) {
                response.error();
                response.getListMessage().add("Error: La Dirección WIFI de la residencia es incorrecta.");
                return response;
            }
            
            if (!requestIp.equals(userLocalAddress)) {
                response.error();
                response.getListMessage().add("Error: Este celular no le pertenece o no está en la red correcta.");
                return response;
            }

            Optional<EntityAttendance> optionalAttendance = repositoryAttendance.findTopByParentUserOrderByCreated_atDesc(entityUser);
            
            if (optionalAttendance.isPresent()) {
                EntityAttendance lastAttendance = optionalAttendance.get();
                
                if (lastAttendance.getStatus()) {
                    lastAttendance.setDepartureDate(new java.sql.Date(new Date().getTime()));
                    lastAttendance.setStatus(false); 
                    lastAttendance.setUpdated_at(new java.sql.Date(new Date().getTime()));
                    
                    repositoryAttendance.save(lastAttendance); 
                    
                    responsetoPython.success();
                    responsetoPython.getListMessage().add("Salida registrada correctamente (Línea completada).");
                    return responsetoPython;
                }
            }
            
            EntityAttendance newAttendance = new EntityAttendance();
            newAttendance.setIdAtendance(UUID.randomUUID().toString());
            newAttendance.setParentUser(entityUser);
            newAttendance.setEntryDate(new java.sql.Date(new Date().getTime())); 
            newAttendance.setStatus(true); 
            newAttendance.setCreated_at(new java.sql.Date(new Date().getTime()));
            
            repositoryAttendance.save(newAttendance); 
            
            responsetoPython.success();
            responsetoPython.getListMessage().add("Entrada registrada correctamente (Nueva línea).");
            return responsetoPython;
            
        } catch (Exception e) {
            e.printStackTrace(); 
            response.error();
            response.getListMessage().add("Error: El servicio no funciona por el momento.");
            return response;
        } finally {
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}
