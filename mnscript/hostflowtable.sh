curl -d '{"switch": "00:00:00:00:00:00:00:01", "name":"flow-mod-1", "cookie":"0", "priority":"32768", "eth_type":"0x86dd", "ipv6_dst":"1111::2","active":"true", "instruction_apply_actions":"set_field=eth_dst->00:00:00:00:00:01,output=3"}' http://localhost:8080/wm/staticflowpusher/json
curl -d '{"switch": "00:00:00:00:00:00:00:02", "name":"flow-mod-2", "cookie":"0", "priority":"32768", "eth_type":"0x86dd", "ipv6_dst":"2222::2","active":"true", "instruction_apply_actions":"set_field=eth_dst->00:00:00:00:00:02,output=3"}' http://localhost:8082/wm/staticflowpusher/json
curl -d '{"switch": "00:00:00:00:00:00:00:03", "name":"flow-mod-3", "cookie":"0", "priority":"32768", "eth_type":"0x86dd", "ipv6_dst":"3333::2","active":"true", "instruction_apply_actions":"set_field=eth_dst->00:00:00:00:00:03,output=3"}' http://localhost:8084/wm/staticflowpusher/json
