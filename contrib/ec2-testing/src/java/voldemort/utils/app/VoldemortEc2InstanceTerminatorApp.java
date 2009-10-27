/*
 * Copyright 2009 LinkedIn, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package voldemort.utils.app;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import joptsimple.OptionSet;
import voldemort.utils.Ec2Connection;
import voldemort.utils.TypicaEc2Connection;

public class VoldemortEc2InstanceTerminatorApp extends VoldemortApp {

    public static void main(String[] args) throws Exception {
        new VoldemortEc2InstanceTerminatorApp().run(args);
    }

    @Override
    protected String getScriptName() {
        return "voldemort-ec2instanceterminator.sh";
    }

    @Override
    public void run(String[] args) throws Exception {
        parser.accepts("accessid", "Access ID").withRequiredArg();
        parser.accepts("secretkey", "SecretKey").withRequiredArg();
        parser.accepts("hostnames", "File containing host names").withRequiredArg();
        parser.accepts("force", "Use option to force deletion of *all* instances");

        OptionSet options = parser.parse(args);
        String accessId = getRequiredString(options, "accessid");
        String secretKey = getRequiredString(options, "secretkey");

        Ec2Connection ec2Connection = new TypicaEc2Connection(accessId, secretKey);

        List<String> hostNames = null;
        File hostNamesFile = getInputFile(options, "hostnames");

        if(hostNamesFile != null) {
            hostNames = getHostNamesFromFile(hostNamesFile, true);
        } else if(options.has("force")) {
            // Get all of the instances...
            hostNames = new ArrayList<String>(ec2Connection.getInstances().keySet());
        } else {
            printUsage();
        }

        ec2Connection.deleteInstances(hostNames);
    }

}