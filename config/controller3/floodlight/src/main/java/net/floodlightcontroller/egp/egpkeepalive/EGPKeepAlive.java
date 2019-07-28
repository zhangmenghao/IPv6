package net.floodlightcontroller.egp.egpkeepalive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6AddressWithMask;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.egp.controller.ControllerMain;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.IPv6;
import net.floodlightcontroller.packet.UDP;

public class EGPKeepAlive implements IFloodlightModule, IOFMessageListener,
		IOFSwitchListener {

	private static IFloodlightProviderService floodlightProvider;
	private static IOFSwitchService switchService;
	private static Logger logger;
	private static final String configFileName = "target/config.txt";
	protected static ControllerMain controllerMain;
	protected static HashMap<String, Long> timermap = new HashMap<String, Long>();
	protected static HashMap<String, Boolean> statusmap = new HashMap<String, Boolean>();
	
	
	public static HashMap<String, Long> getTimermap() {
		return timermap;
	}

	public static void setTimermap(HashMap<String, Long> timermap) {
		EGPKeepAlive.timermap = timermap;
	}

	public static HashMap<String, Boolean> getStatusmap() {
		return statusmap;
	}

	public static void setStatusmap(HashMap<String, Boolean> statusmap) {
		EGPKeepAlive.statusmap = statusmap;
	}

	@Override
	public String getName() {
		return EGPKeepAlive.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void switchAdded(DatapathId switchId) {
		logger.info("Switch {} connected; processing its static entries",switchId.toString());
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchActivated(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port,
			PortChangeType type) {
		// TODO Auto-generated method stub

	}

	@Override
	public void switchChanged(DatapathId switchId) {
		// TODO Auto-generated method stub

	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(
			IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
		switch (msg.getType()) {
        	case PACKET_IN:
        		OFPacketIn myPacketIn = (OFPacketIn) msg;
        		OFPort myInPort = (myPacketIn.getVersion().compareTo(OFVersion.OF_12) < 0) 
        		? myPacketIn.getInPort() : myPacketIn.getMatch().get(MatchField.IN_PORT);
        		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,
        				IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        		if (eth.getEtherType() == EthType.IPv6){
        			IPv6 ipv6 = (IPv6) eth.getPayload();
        			if (ipv6.getNextHeader() == IpProtocol.UDP){
        				UDP udp = (UDP) ipv6.getPayload();
        				TransportPort srcPort = udp.getSourcePort();
        				TransportPort dstPort = udp.getDestinationPort();
        				if (srcPort.equals(TransportPort.of(30001)) && dstPort.equals(TransportPort.of(30002))){
        					Data data = (Data) udp.getPayload();
            				byte[] databyte = data.getData();
            				String datastring = databyte.toString();
            				String infostr = String.format("Packet in: inport: %s seen on switch: %s, Payload: %s", 
            						myInPort.toString(), sw.getId().toString(), datastring);
            				logger.info(infostr);
            				String switchport = sw.getId().toString() + ": " + myInPort.toString();
            				long currentTime = System.currentTimeMillis();
            				timermap.put(switchport, Long.valueOf(currentTime));
        				}
        			}
        		}
        		
        break;
            default:
                break;
		}
		return Command.CONTINUE;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
		        new ArrayList<Class<? extends IFloodlightService>>();
		    l.add(IFloodlightProviderService.class);
		    l.add(IOFSwitchService.class);
		    return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		logger = LoggerFactory.getLogger("egpnew ControllerMain(configFileName).work();.egpkeepalive.EGPKeepAlive");
		controllerMain = new ControllerMain(configFileName);
	}

	@Override
	public void startUp(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		switchService.addOFSwitchListener(this);
		controllerMain.work();
	}
	
	public static void SendPacketOut(String switchid, int outport) {
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		if (mySwitch == null)
			return;
		OFFactory myFactory = mySwitch.getOFFactory();		
		/* Compose L2 packet. */
		Ethernet eth = new Ethernet();
		eth.setSourceMACAddress(MacAddress.of("10:00:00:00:00:00"));
		eth.setDestinationMACAddress(MacAddress.of("11:00:00:00:00:00"));
		eth.setEtherType(EthType.IPv6);
	 
		/* Compose L3 packet. */
		/*IPv4 ipv4 = new IPv4();
		ipv4.setSourceAddress(IPv4Address.of("127.0.0.1"));
		ipv4.setDestinationAddress(IPv4Address.of("127.0.0.1"));
		ipv4.setProtocol(IpProtocol.UDP);*/
		IPv6 ipv6 = new IPv6();
		ipv6.setSourceAddress(IPv6Address.of("::1"));
		ipv6.setDestinationAddress(IPv6Address.of("::1"));
		ipv6.setNextHeader(IpProtocol.UDP);
		
		/* Compose L4 packet. */
		UDP udp = new UDP();
		udp.setSourcePort(TransportPort.of(30001));
		udp.setDestinationPort(TransportPort.of(30002));
		
		/* Compose L5 packet. */
		Data data = new Data();
		String Keepalivedata = "Keep Alive!";
		byte[] Keepalivebyte = Keepalivedata.getBytes();
		data.setData(Keepalivebyte);
				 
		/* Set L2 L3 L4's payload */
		/*eth.setPayload(ipv4);
		ipv4.setPayload(udp);*/
		eth.setPayload(ipv6);
		ipv6.setPayload(udp);
		udp.setPayload(data);

		 
		/* Specify the switch port(s) which the packet should be sent out. */
		OFActionOutput output = myFactory.actions().buildOutput()
		    .setPort(OFPort.of(outport))
		    .build();

		/* 
		 * Compose the OFPacketOut with the above Ethernet packet as the 
		 * payload/data, and the specified output port(s) as actions.
		 */
		OFPacketOut myPacketOut = myFactory.buildPacketOut()
		    .setData(eth.serialize())
		    .setBufferId(OFBufferId.NO_BUFFER)
		    .setActions(Collections.singletonList((OFAction) output))
		    .build();
		 
		/* Write the packet to the switch via an IOFSwitch instance. */
		mySwitch.write(myPacketOut);
	}
	
	public static void createFlowMods(String switchid, String srcipv6, String dstipv6, 
			String protocol, String srcport, String dstport, int outport){
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		OFFactory myFactory = mySwitch.getOFFactory();
		OFVersion myVersion = myFactory.getVersion();
		
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv6);
		if (srcipv6 != null)
			myMatchBuilder.setMasked(MatchField.IPV6_SRC, IPv6AddressWithMask.of(srcipv6));
		if (dstipv6 != null)
			myMatchBuilder.setMasked(MatchField.IPV6_DST, IPv6AddressWithMask.of(dstipv6));
		if (protocol != null){
			if (protocol.equalsIgnoreCase("tcp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.TCP_DST, TransportPort.of(Integer.parseInt(dstport)));
			}		
			if (protocol.equalsIgnoreCase("udp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.UDP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.UDP_DST, TransportPort.of(Integer.parseInt(dstport)));
			}
		}
		
		
		Match myMatch = myMatchBuilder.build();
		
		switch (myVersion){
			case OF_10:
				ArrayList<OFAction> actionList10 = new ArrayList<OFAction>();
				OFActions actions10 = myFactory.actions();
				
				// Use builder again.
				OFActionOutput output = actions10.buildOutput()
				    .setMaxLen(0xFFFFFFFF)
				    .setPort(OFPort.of(outport))
				    .build();
				actionList10.add(output);
				
				
				OFFlowAdd flowAdd10 = myFactory.buildFlowAdd()
					    .setBufferId(OFBufferId.NO_BUFFER)
					    .setHardTimeout(3600)
					    .setIdleTimeout(3600)
					    .setPriority(32768)
					    .setMatch(myMatch)
					    .setActions(actionList10)
					    .setOutPort(OFPort.of(outport))
					    .build();
				
				mySwitch.write(flowAdd10);
				break;
			default:
				logger.error("Unsupported OFVersion: {}", myVersion.toString());
				break;
		}
		
	}
	
	public static void deleteFlowMods(String switchid, String srcipv6, String dstipv6, 
			String protocol, String srcport, String dstport, int outport){
		IOFSwitch mySwitch = switchService.getSwitch(DatapathId.of(switchid));
		OFFactory myFactory = mySwitch.getOFFactory();
		
		Match.Builder myMatchBuilder = myFactory.buildMatch();
		myMatchBuilder.setExact(MatchField.ETH_TYPE, EthType.IPv6);
		if (srcipv6 != null)
			myMatchBuilder.setMasked(MatchField.IPV6_SRC, IPv6AddressWithMask.of(srcipv6));
		if (dstipv6 != null)
			myMatchBuilder.setMasked(MatchField.IPV6_DST, IPv6AddressWithMask.of(dstipv6));
		if (protocol != null){
			if (protocol.equalsIgnoreCase("tcp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.TCP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.TCP_DST, TransportPort.of(Integer.parseInt(dstport)));
			}		
			if (protocol.equalsIgnoreCase("udp")){
				myMatchBuilder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
				if (srcport != null)
					myMatchBuilder.setExact(MatchField.UDP_SRC, TransportPort.of(Integer.parseInt(srcport)));
				if (dstport != null)
					myMatchBuilder.setExact(MatchField.UDP_DST, TransportPort.of(Integer.parseInt(dstport)));
			}
		}
		
		Match myMatch = myMatchBuilder.build();
		
		OFFlowDelete flowDelete = myFactory.buildFlowDelete()
				.setBufferId(OFBufferId.NO_BUFFER)
				.setHardTimeout(3600)
				.setIdleTimeout(3600)
				.setPriority(32768)
				.setMatch(myMatch)
				.build();
				
		mySwitch.write(flowDelete);
	}

}
