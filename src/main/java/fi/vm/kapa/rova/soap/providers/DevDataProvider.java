package 
fi.vm.kapa.rova.soap.providers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.ws.Holder;

import fi.vm.kapa.xml.rova.api.AuthorizationType;
import fi.vm.kapa.xml.rova.api.DecisionReasonType;
import fi.vm.kapa.xml.rova.api.ObjectFactory;
import fi.vm.kapa.xml.rova.api.Principal;
import fi.vm.kapa.xml.rova.api.PrincipalType;

public class DevDataProvider implements DataProvider {
	
	private Map<String, PrincipalItem> principalData = null;
	private Map<String, List<String>> authorizationData = new HashMap<String, List<String>>();
	private Map<String, String> children = new HashMap<String, String>();
	private Map<String, String> parents = new HashMap<String, String>();
	private Map<String, String> grandparents = new HashMap<String, String>();
	

	private ObjectFactory factory = new ObjectFactory();

	public DevDataProvider() {
		populateChildData();
		populateParentData();
		populateGrandParentData();
		populatePrincipalData();
		populateAuthorizationData();
	}
	
	public AuthorizationType getAuthorizationTypeResponse(String hetu,
			String target, String industry, String service, String issue, String endUserId, Holder<List<DecisionReasonType>> reason) {
	
		List<String> currentAuth = authorizationData.get(hetu);
		if (currentAuth != null && currentAuth.contains(target)) {
			return AuthorizationType.ALLOWED;
		} else {
			return AuthorizationType.DISALLOWED;
		}
	}
	public Principal getPrincipalResponse(String hetu, String industry,
			String service, String issue, String endUserId, Holder<List<DecisionReasonType>> reason) {
		if (principalData.containsKey(hetu)) {
			PrincipalItem item = principalData.get(hetu);
			Principal principal = factory.createPrincipal();
			if (item.guardianship)
				principal.setDelegationResponse(AuthorizationType.DISALLOWED);
			for (String key : item.children.keySet()) {
				principal.getPrincipalElem().add(getType(key, item.children.get(key)));
			}
						
			return principal;
		} else {
			return null;
		}
	}
	
	private void populateGrandParentData() {
		grandparents.put("280146-064T", "Taina Luttinen"); // mummo (Pauliina)
		grandparents.put("130744-319N", "Viljo Luttinen"); // pappa (Pauliina)
		grandparents.put("220440-489H", "Eino Kumpulainen"); // mummi (Matti)
		grandparents.put("201143-544D", "Anja Kumpulainen"); // vaari (Matti)
	}

	private void populateParentData() {
		parents.put("010180-123F", "Pauliina Kumpulainen"); // äiti
		parents.put("071082-399L", "Matti Kumpulainen"); // isä
		parents.put("100886-508F", "Tiina Laakso"); // ex-äiti
		parents.put("210263-999M", "Topias Kuitunen"); // rov1: 2 x huollettava
		parents.put("170479-999T", "Hans Ollila"); // rov2: turvakielto
		parents.put("080518-9981", "Tony Tester"); // rov1: edunvalvonta
		
	}

	private void populateChildData() {
		children.put("260111A7088", "Titta Kumpulainen");
		children.put("180910A061D", "Onni Kumpulainen");
		children.put("101106A281X", "Jani Kumpulainen");
		children.put("151199-999A", "Joel Kuitunen");
		children.put("180897-999A", "Weeti Kuitunen");
	}

	
	private void populatePrincipalData() {
		
		principalData = new HashMap<String, PrincipalItem>();
		
		PrincipalItem item = new PrincipalItem();
		item.hetu = "010180-123F";
		item.name = parents.get("010180-123F"); //"Pauliina Kumpulainen";
		item.children.put("260111A7088", children.get("260111A7088")); //"Titta Kumpulainen"
		item.children.put("180910A061D", children.get("180910A061D")); // "Onni Kumpulainen"
		item.children.put("101106A281X", children.get("101106A281X")); // "Jani Kumpulainen"
//		item.children.put("130744-319N", grandparents.get("130744-319N")); // "Viljo Luttinen"
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "071082-399L";
		item.name = parents.get("071082-399L"); //"Matti Kumpulainen";
		item.children.put("260111A7088", children.get("260111A7088")); //"Titta Kumpulainen"
		item.children.put("180910A061D", children.get("180910A061D")); // "Onni Kumpulainen"
		item.children.put("101106A281X", children.get("101106A281X")); // "Jani Kumpulainen"
//		item.children.put("201143-544D", grandparents.get("201143-544D")); // "Anja Kumpulainen"
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "100886-508F";
		item.name = parents.get("100886-508F"); //"Tiina Laakso";
		item.children.put("101106A281X", children.get("101106A281X")); // "Jani Kumpulainen"
//		item.children.put("101106A283Z", "Pasi Laakso"); // "Pasi Laakso"; ei kaaviossa
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "280146-064T";
		item.name = grandparents.get("280146-064T"); //"Taina Luttinen" mummo;
		item.children.put("010180-123F", parents.get("010180-123F")); // "Pauliina Kumpulainen"
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "130744-319N";
		item.name = grandparents.get("130744-319N"); //"Viljo Luttinen" pappa;
		item.children.put("010180-123F", parents.get("010180-123F")); // "Pauliina Kumpulainen"
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "201143-544D";
		item.name = grandparents.get("201143-544D"); //"Anja Kumpulainen" mummi;
		item.children.put("071082-399L", parents.get("071082-399L")); // "Matti Kumpulainen"
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "220440-489H";
		item.name = grandparents.get("220440-489H"); //"Eino Kumpulainen" vaari;
		item.children.put("071082-399L", parents.get("071082-399L")); // "Matti Kumpulainen"
		principalData.put(item.hetu, item);

		item = new PrincipalItem();
		item.hetu = "210263-999M";
		item.name = parents.get("210263-999M"); //"Topias Kuitunen" 2 x huollettava;
		item.children.put("151199-999A", children.get("151199-999A")); //"Joel Kuitunen"
		item.children.put("180897-999A", children.get("180897-999A")); // "Weeti Kuitunen"
		principalData.put(item.hetu, item);
		
		item=new PrincipalItem();
		item.hetu="080518-9981";
		item.name=parents.get("080518-9981"); //Tony tester - Edunvalvonnassa
		item.guardianship=true;
		item.children.put("180897-999A", children.get("180897-999A"));
		principalData.put(item.hetu, item);
	}
	
	private PrincipalType getType(String hetu, String name) {
		PrincipalType pt = factory.createPrincipalType();
		pt.setTargetIdentifier(hetu);
		pt.setTargetName(name);
		return pt;
	}

	private void populateAuthorizationData() {
		List<String> list = new ArrayList<String>();
		list.add("260111A7088"); // Titta Kumpulainen
		list.add("180910A061D"); // Onni Kumpulainen
		this.authorizationData.put("010180-123F", list); // Pauliinan valtuutetut

		list = new ArrayList<String>();
		list.add("260111A7088"); // Titta Kumpulainen
		list.add("180910A061D"); // Onni Kumpulainen
		list.add("101106A281X"); // Jani Kumpulainen
		this.authorizationData.put("071082-399L", list); // Matin valtuutetut

		list = new ArrayList<String>();
		list.add("101106A281X"); // Jani Kumpulainen
		this.authorizationData.put("100886-508F", list); // Tiinan valtuutetut
		
		list = new ArrayList<String>();
		list.add("151199-999A"); // Joel Kuitunen
		list.add("180897-999A"); // Weeti Kuitunen
		this.authorizationData.put("210263-999M", list); // Topiaksen valtuutetut
	}
	
	
	private class PrincipalItem {
		private String hetu;
		private String name;
		private boolean guardianship;
		private Map<String, String> children = new HashMap<String, String>();
	}
	
}
