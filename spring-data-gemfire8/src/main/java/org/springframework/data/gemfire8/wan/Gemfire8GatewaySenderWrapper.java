package org.springframework.data.gemfire8.wan;

import org.springframework.data.gemfire.wan.GatewaySenderWrapper;

import com.gemstone.gemfire.cache.wan.GatewayEventSubstitutionFilter;
import com.gemstone.gemfire.cache.wan.GatewaySender;

public class Gemfire8GatewaySenderWrapper extends GatewaySenderWrapper {

	public Gemfire8GatewaySenderWrapper(GatewaySender gatewaySender) {
		super(gatewaySender);
	}
	
	@SuppressWarnings("rawtypes")
	public GatewayEventSubstitutionFilter getGatewayEventSubstitutionFilter() {
		return getDelegate().getGatewayEventSubstitutionFilter();
	}
	
	@Override
	public int getMaxParallelismForReplicatedRegion() {
		return getDelegate().getMaxParallelismForReplicatedRegion();
	}

}
