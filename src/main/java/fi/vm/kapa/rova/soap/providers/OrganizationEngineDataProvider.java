package fi.vm.kapa.rova.soap.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import fi.vm.kapa.rova.config.SpringProperties;
import fi.vm.kapa.rova.logging.Logger;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationListType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationType;
import fi.vm.kapa.xml.rova.api.orgroles.OrganizationalRolesType;
import fi.vm.kapa.xml.rova.api.orgroles.RoleList;

@Component
public class OrganizationEngineDataProvider implements OrganizationDataProvider, SpringProperties {

    Logger LOG = Logger.getLogger(OrganizationEngineDataProvider.class, Logger.SOAP_SERVICE);

    private static fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory organizationalRolesFactory = new fi.vm.kapa.xml.rova.api.orgroles.ObjectFactory();
    
    // TODO to be removed after Engine and Virre-client are working
    private static Map<String, List<OrganizationalRolesType>> ortMap = new HashMap<String, List<OrganizationalRolesType>>();
    
    @Value(REQUEST_ALIVE_SECONDS)
    private Integer requestAliveSeconds;

    public void handleOrganizationalRoles(String personId, List<String> organizationIds, String service,
            String endUserId, String requestId,
            Holder<fi.vm.kapa.xml.rova.api.orgroles.Response> responseHolder) {
     
        if (responseHolder.value == null) {
            responseHolder.value = organizationalRolesFactory.createResponse();
        }
        
        OrganizationListType organizationListType = organizationalRolesFactory.createOrganizationListType();

        responseHolder.value.setOrganizationList(organizationListType);
        
        List<OrganizationalRolesType> organizationalRoles = organizationListType.getOrganization();
        
        // TODO to be removed after Engine and Virre-client are working
        setDummySoapData(personId, organizationIds, organizationalRoles);
    }
    
    @Override
    public String toString() {
        return "OrganizationalEngineDataProvider";
    }
    
    // TODO to be removed after Engine and Virre-client are working
    private void setDummySoapData(String personId, List<String> organizationIds, List<OrganizationalRolesType> organizationalRoles) {
        if (personId == null || "".equals(personId.trim())) {
            return;
        }
        
        List<OrganizationalRolesType> ortList = ortMap.get(personId);
        if (ortList != null) {
            if (organizationIds != null && organizationIds.size() > 0) {
                for (OrganizationalRolesType organizationalRolesType : ortList) {
                    if (organizationIds.contains(organizationalRolesType.getOrganization().getOrganizationIdentifier())) {
                        organizationalRoles.add(organizationalRolesType);
                    }
                }
            } else {
                for (OrganizationalRolesType organizationalRolesType : ortList) {
                    organizationalRoles.add(organizationalRolesType);
                }
            }
        }
    }

    // TODO to be removed after Engine and Virre-client are working
    static {
        List<OrganizationalRolesType> ortList = new ArrayList<OrganizationalRolesType>();
        OrganizationalRolesType roleType = new OrganizationalRolesType();
        OrganizationType organizationType = organizationalRolesFactory.createOrganizationType();
        RoleList roleList = organizationalRolesFactory.createRoleList();
        organizationType.setOrganizationIdentifier("2560102-1");
        organizationType.setName("Kesäpojjaat Oy");
        roleList.getRole().add("ALL");
        roleType.setOrganization(organizationType);
        roleType.setRoles(roleList);
        ortList.add(roleType);
        
        roleType = new OrganizationalRolesType();
        organizationType = organizationalRolesFactory.createOrganizationType();
        roleList = organizationalRolesFactory.createRoleList();
        organizationType.setOrganizationIdentifier("2667328-9");
        organizationType.setName("Testilafka Oy");
        roleList.getRole().add("ALL");
        roleList.getRole().add("TJ");
        roleType.setOrganization(organizationType);
        roleType.setRoles(roleList);
        ortList.add(roleType);
        
        ortMap.put("010180-9026", ortList); // Pauliina K.
        
        ortList = new ArrayList<OrganizationalRolesType>();
        roleType = new OrganizationalRolesType();
        organizationType = organizationalRolesFactory.createOrganizationType();
        roleList = organizationalRolesFactory.createRoleList();
        organizationType.setOrganizationIdentifier("4235468-3");
        organizationType.setName("Matin MakroSofta");
        roleList.getRole().add("ALL");
        roleType.setOrganization(organizationType);
        roleType.setRoles(roleList);
        ortList.add(roleType);
        
        roleType = new OrganizationalRolesType();
        organizationType = organizationalRolesFactory.createOrganizationType();
        roleList = organizationalRolesFactory.createRoleList();
        organizationType.setOrganizationIdentifier("1252786-2");
        organizationType.setName("KRP-Parts");
        roleList.getRole().add("TJ");
        roleType.setOrganization(organizationType);
        roleType.setRoles(roleList);
        ortList.add(roleType);

        ortMap.put("120978-9038", ortList); // Matti K.
    }
  
}
