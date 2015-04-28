package fi.vm.kapa.rova.soap.model;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class ClientHeader {

	@XmlElement(namespace = "http://x-road.eu/xsd/identifiers", name = "sdsbInstance")
	private String instance;

	@XmlElement(namespace = "http://x-road.eu/xsd/identifiers", name = "memberClass")
	private String memberClass;

	@XmlElement(namespace = "http://x-road.eu/xsd/identifiers", name = "memberCode")
	private String memberCode;

	@XmlElement(namespace = "http://x-road.eu/xsd/identifiers", name = "subsystemCode")
	private String subsystemCode;

	public String getServiceName() {
		StringBuilder sb = new StringBuilder();
		sb.append(instance);
		sb.append("_");
		sb.append(memberClass);
		sb.append("_");
		sb.append(memberCode);
		sb.append("_");
		sb.append(subsystemCode);
		return sb.toString();
	}
	
	@Override
	public String toString() {
		return "ClientHeader [instance=" + instance + ", memberClass="
				+ memberClass + ", memberCode=" + memberCode
				+ ", subsystemCode=" + subsystemCode + "]";
	}
}
