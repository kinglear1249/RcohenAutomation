import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic 


def get_real_ne_setup_number(String job_name)
{
    def func_name = "get_real_ne_setup_number - "
    println(func_name + "got job_name:" + job_name)
    def values = job_name.split('_')
    def arr_size = values.size()
    def setup_name = values[arr_size -1]
    if (setup_name == 'SFA')
    {
        setup_number = '23'
    } else if (setup_name == '2100')
    {
        setup_number = '50'
    } else if (setup_name == '2300')
    {
        setup_number = '51'
    } else if (setup_name == '2400')
    {
        setup_number = '52'
    } else if (setup_name == '2700')
    {
        setup_number = '53'
    }
    else
    {
        setup_number = values[arr_size -1]
    }
    println("The setup number is: " + setup_number)
    return setup_number
}

def get_desired_test_suite_robot_variable(String desired_suite, String desired_single_test, String costum_suite)
{
    def func_name = "get_desired_test_suite_robot_variable - "
    def desired_test_suite_robot_variable = " --include "
    //println(func_name + "desired suite to run is:" + desired_suite)
    if (! desired_suite.equalsIgnoreCase("none"))
    {
        if (desired_suite == "v9_0G1")
        {
            desired_test_suite_robot_variable = " -i l3vpn -i layer2Service -i rsvpTe"
        }
        else if (desired_suite == "v9_0G2")
        {
            desired_test_suite_robot_variable = " -i evpn -i igp -i pece "
        }
        else if (desired_suite == "v9_0G3")
        {
            desired_test_suite_robot_variable = " --include scale "
        }
        else if (desired_suite == "v9_0G4")
        {
            desired_test_suite_robot_variable = " --include srTe "
        }
        else if (desired_suite == "v9_0G5")
        {
            desired_test_suite_robot_variable = " --include l3vpn "
        }
        else if (desired_suite == "v9_0G6")
        {
            desired_test_suite_robot_variable = " --include sfa "
        }
        else if (desired_suite == "v9_0G7")
        {
            desired_test_suite_robot_variable = " --include l3vpn "
        }
        else if (desired_suite == "v9_1G8")
        {
            desired_test_suite_robot_variable = " -i l3vpn -i layer2Service "
        }
        else if (desired_suite == "v9_1G9")
        {
            desired_test_suite_robot_variable = " -i evpn -i igp -i pece "
        }
        else
        {
            desired_test_suite_robot_variable += desired_suite + " "
        }
    }
    else if (desired_single_test != null && !desired_single_test.isEmpty())
    {
        desired_test_suite_robot_variable += desired_single_test + " "
    }
    else if (costum_suite != null && !costum_suite.isEmpty())
    {
        desired_test_suite_robot_variable += costum_suite + " "
    }
    else
    {
        String err_msg = "the desired test suite is not valid"
        println(func_name + err_msg)
        error(err_msg)
    }
    return desired_test_suite_robot_variable
}


def create_virtual_environment(String virtual_env_name)
{
    def func_name = "create_virtual_environment - "
    println(func_name + "PATH environment variable is:" + env.PATH)
    println(func_name + "WORKSPACE environment variable is:" + env.WORKSPACE)
    println(func_name + "the virtual environment name to create is:" + virtual_env_name)
    def String python_ver_to_use = "python3"
    println(func_name + "about to use Python from the following path:" + python_ver_to_use)
    sh """
        echo "ls is:"
        ls
        echo "virtual environment directory name is:${virtual_env_name}"
        if [ -d ${virtual_env_name} ]; then
            echo "directory ${virtual_env_name} exist, removing it"
            rm -rf ${virtual_env_name}
	    else
            echo "directory ${virtual_env_name} does NOT exist, no need to remove it"
        fi

        ${python_ver_to_use} -m venv ${virtual_env_name}
    """
}

def install_automation_project(String virtual_env_name)
{
    def func_name = "install_automation_project - "
    def pip_conf_file_name = "pip.conf"
    println(func_name + "got virtual environment directory name:" + virtual_env_name)
    sh """
        export https_proxy=
        export http_proxy=
        echo "copying the pip.conf file to the virtual environment folder"
        cp ${pip_conf_file_name} ${virtual_env_name}
	    echo "activating the ${virtual_env_name} virtual environment"
		. ${virtual_env_name}/bin/activate
		echo "installing the project using the pip3 install -r requirements.txt command followed by the pip3 install . command"
        pip3 install -q -r requirements.txt
        python3 setup.py -q install
        pip3 list
        echo "done installing the project using the pip3 commands"
    """
// alternative for the Nexus installation command: pip3 install ixiarestpyapiwrapper --index-url http://ibiza.ecitele.com:9081/repository/pypi-tango-all/simple --index http://ibiza.ecitele.com:9081/repository/pypi-tango-all/pypi --trusted-host ibiza.ecitele.com
}

def compose_npt_automation_tests_command_line(String setup_number, String workspace, String test_suite_robot_var,
                                              String is_gr_enabled, String should_stop_on_fail, String with_traffic,
                                              String stability, String delete_previous_setup_config)
{
    def func_name = "compose_robot_command - "
    println(func_name + "got setup number:" + setup_number)
    def setup_number_part_in_command = " --variable setup_number:" + setup_number
    def automation_project_root_dir_part_in_command = " --variable automation_project_root_dir:" + workspace
    def workspace_path_part_in_command = " --variable workspace_path:" + workspace
    def is_gr_enabled_part_in_command = " --variable is_gr_enabled:" + is_gr_enabled
    def should_stop_on_fail_part_in_command = ""
    if (should_stop_on_fail.equalsIgnoreCase("yes"))
    {
        should_stop_on_fail_part_in_command = " --exitonfailure"
    }
    def with_traffic_part_in_command = " --variable with_traffic:" + with_traffic
    def stability_part_in_command = " --variable stability:" + stability
    def delete_previous_setup_config_part_in_command = " --variable delete_previous_setup_config:" + delete_previous_setup_config
    // if the versions.json file was created in advance, go ahead and use it for the
    // --logtitle robot built in command lien argument
    def String versions_file_name = "dswpAndStversions.json"
    def versions_file_path = workspace + "/" + versions_file_name
    println("The versions file path is: \n" + versions_file_path)
    def file = new File(versions_file_path)
    def String versions_part_in_command = ""
//     sh """
//         echo "list2 all files in temporary workspace before running tests"
//         ls -la ${workspace}
//     """
    try
    {
        def data = readFile(file: versions_file_path)
        println("When reading the dswp and St versions file with readFile method the content is:")
        println(data)
    }
    catch (e)
    {
        println("There was an error trying to read form file:" + versions_file_path)
        def String errorMsg = e.toString()
        println("caught exception with error:" + errorMsg)
        // TODO: change the pipelines that use this function to consider the return
        // value of this function (that was not consider before)
        return ""
    }
    def data = readFile(file: versions_file_path)
    def jsonSlurper = new JsonSlurper()
    def json_data = jsonSlurper.parseText(data)
    println(func_name + "the JSON file parsed has the following content:" + json_data)
    dswp_version = json_data.DSWP
    st_version = json_data.ST
    dswp_ver_part_in_log_title_after_removal = dswp_version.replaceAll(" ","-")
    println(func_name + "extracted DSWP version:" + dswp_ver_part_in_log_title_after_removal + " and ST version:" + st_version)
    versions_part_in_command = ""
    if (!dswp_version.isEmpty() && !st_version.isEmpty())
    {
        versions_part_in_command = "DSWP:" + dswp_ver_part_in_log_title_after_removal + "__ST:" + st_version
    }

    def log_title_part_in_command = ""
    if (!versions_part_in_command.isEmpty())
    {
        log_title_part_in_command = " --logtitle " + versions_part_in_command
    }
    else
    {
        println("The versions part in log title is empty")
    }
    def output_dir_part_in_command = " --outputdir=" + workspace
    def debug_file_part_in_command = " --debugfile=" + workspace + "/nptAutomationLog.txt"
    def test_suite_part_in_command = test_suite_robot_var + " tests"
    def final_robot_command = "robot" + setup_number_part_in_command + automation_project_root_dir_part_in_command + workspace_path_part_in_command + is_gr_enabled_part_in_command  + should_stop_on_fail_part_in_command + with_traffic_part_in_command + stability_part_in_command + delete_previous_setup_config_part_in_command + output_dir_part_in_command + log_title_part_in_command + debug_file_part_in_command + test_suite_part_in_command
    println(func_name + "the robot command that is about to be invoked is:" + final_robot_command)
    // robot --variable setup_number:${setup_number_to_use} --variable automation_project_root_dir:${WORKSPACE} --variable workspace_path:${WORKSPACE} --variable is_gr_enabled:false --outputdir=${WORKSPACE} --debugfile=${WORKSPACE}/automationLog.txt ${test_suite_robot_var} tests
    return final_robot_command
}

def compose_bsp_os_automation_tests_command_line(String setup_number, String workspace_path, String bsp_os_data_file,
                                             String test_suite_robot_var, String should_stop_on_fail)
{
    def func_name = "compose_bsp_os_tests_command_line - "
    println(func_name + "got setup number:" + setup_number)
    def String robot_command = "robot --variable setup_number:" + setup_number + " --variable suite_type:BspOsAutomation" + " --variable automation_project_root_dir:" + automation_project_root_dir
    robot_command += " --variable workspace_path:" + workspace_path
    if(bsp_os_data_file != null && !bsp_os_data_file.isEmpty())
    {
        robot_command += " --variable bsp_os_data_file:" + bsp_os_data_file
    }
    if (should_stop_on_fail.equalsIgnoreCase("yes"))
    {
        robot_command += " --exitonfailure"
    }
    robot_command += " --outputdir=" + workspace_path
    robot_command += " --debugfile=" + workspace_path + "/bspOsAutomationLog.txt"
    def test_suite_part_in_command = test_suite_robot_var + " tests"
    robot_command += test_suite_part_in_command
    return robot_command
}

def run_robot_command(String robot_command, String virtual_env_name, String number_of_times_to_run_tests)
{
    def func_name = "run_robot_command - "
    println(func_name + "the number of tests iterations is:" + number_of_times_to_run_tests)
    int num_iterations = number_of_times_to_run_tests.toInteger()
    if (num_iterations < 1 || num_iterations > 500)
    {
        String error_msg = "the number of tests iterations is invalid"
        println(func_name + error_msg)
        error(error_msg)
    }
    println(func_name + "got the following robot command to use to invoke tests run:" + robot_command)
    println(func_name + "got the virtual environment directory name:" + virtual_env_name)
    for(i = 0; i < number_of_times_to_run_tests.toInteger();)
    {
        println("running the command:" + robot_command + " for the:" + (++i).toString() + " time")
        sh """
            echo "activating the ${virtual_env_name} virtual environment"
            . ${virtual_env_name}/bin/activate
            echo "the test suite to run is:${params.TestSuite}"
            ${robot_command}
        """
    }
    println(func_name + "done running tests via the Robot framework")
}

def compose_get_ne_versions_command_line(String setup_number, String automation_project_root_dir, String workspace_path)
{
    func_name = "compose_get_ne_versions_command_line - "
    println(func_name + "about to compose the robot command on setup:" + setup_number)
    def String robot_command = "robot --variable setup_number:" + setup_number \
                                + " --variable suite_type:GetNptNeVersions" \
                                + " --variable automation_project_root_dir:" + automation_project_root_dir
    robot_command += " --variable workspace_path:" + workspace_path
    robot_command += " --outputdir=" + workspace_path
    robot_command += " --debugfile=" + workspace_path + "/getNeVersionsLog.txt"
    // add the get NE version suite
    String get_ne_versions_suite = "utilities.getNeVersionsUtility tests"
    robot_command += " --suite " + get_ne_versions_suite
    return robot_command
}

def compose_npt_deploy_command_line(String setup_number, String automation_project_root_dir, String workspace_path,
                                    String fsl_ppc_dswp_file, String intel_x86_dswp_file,
                                    String fsl_ppc_st_files, String intel_x86_st_files,
                                    String delete_previous_setup_config)
{
    func_name = "compose_npt_deploy_command_line - "
    println(func_name + "about to compose the robot command on setup:" + setup_number)
    def String robot_command = "robot --variable setup_number:" + setup_number + " --variable suite_type:NptDeploy"  + " --variable automation_project_root_dir:" + automation_project_root_dir
    robot_command += " --variable workspace_path:" + workspace_path
    if(fsl_ppc_dswp_file != null && !fsl_ppc_dswp_file.isEmpty())
    {
        robot_command += " --variable fsl_ppc_dswp_file:" + fsl_ppc_dswp_file
    }
    if(intel_x86_dswp_file != null && !intel_x86_dswp_file.isEmpty())
    {
        robot_command += " --variable intel_x86_dswp_file:" + intel_x86_dswp_file
    }
    if(fsl_ppc_st_files != null && !fsl_ppc_st_files.isEmpty())
    {
        robot_command += " --variable fsl_ppc_st_files:" + fsl_ppc_st_files
    }
    if(intel_x86_st_files != null && !intel_x86_st_files.isEmpty())
    {
        robot_command += " --variable intel_x86_st_files:" + intel_x86_st_files
    }
    if(delete_previous_setup_config != null && !delete_previous_setup_config.isEmpty())
    {
        robot_command += " --variable delete_previous_setup_config:" + delete_previous_setup_config
    }
    robot_command += " --outputdir=" + workspace_path
    robot_command += " --debugfile=" + workspace_path + "/nptDeployLog.txt"
    // add the HW deployment suite
    String npt_deploy_suite = "utilities.deployHwSetupUtility tests"
    robot_command += " --suite " + npt_deploy_suite
    return robot_command
}

def String getDswpAndStVersionsForCurrentRun(String workspace)
{
    def String versions_file_name = "dswpAndStversions.json"
    def versions_file_path = workspace + "/" + versions_file_name
    def file = new File(versions_file_path)
    String versions_part_in_command = ""
    if (file.exists() && file.canRead())
    {
        def jsonSlurper = new JsonSlurper()
        def json_data = jsonSlurper.parse(new File(versions_file_path))
        println(func_name + "the JSON file parsed has the following content:" + json_data)
        dswp_version = json_data.DSWP
        st_version = json_data.ST
        println(func_name + "extracted DSWP version:" + dswp_version + " and ST version:" + st_version)
        versions_part_in_command = "DSWP:" + dswp_version + "  ST:" + st_version
    }
    return versions_part_in_command
}

def compose_stress_memory_leak_command_line(String automation_project_root_dir, String workspace_path,
                                                  String scenario_file)
{
    func_name = "compose_vil_memory_leak_scenario_command_line - "
    println(func_name + "about to compose the robot test to run VIL memory leak scenario on VIL setup:")
    def String robot_command = "robot --variable automation_project_root_dir:" + automation_project_root_dir + " "
    robot_command += " --variable workspace_path:" + workspace_path
    if(scenario_file != null && !scenario_file.isEmpty())
    {
        robot_command += " --variable scenario_file:" + scenario_file
    }
    robot_command += " --outputdir=" + workspace_path
    robot_command += " --debugfile=" + workspace_path + "/automationLog.txt"
        // if the versions.json file was created in advance, go ahead and use it for the
    // --logtitle robot built in command lien argument
    def String versions_file_name = "dswpAndStversions.json"
    def versions_file_path = workspace + "/" + versions_file_name
    println("The versions file path is: \n" + versions_file_path)
    def file = new File(versions_file_path)
    def String versions_part_in_command = ""
//     sh """
//         echo "list2 all files in temporary workspace before running tests"
//         ls -la ${workspace}
//     """
    try
    {
        def data = readFile(file: versions_file_path)
        println("When reading the dswp and St versions file with readFile method the content is:")
        println(data)
    }
    catch (e)
    {
        println("There was an error trying to read form file:" + versions_file_path)
        def String errorMsg = e.toString()
        println("caught exception with error:" + errorMsg)
        // TODO: change the pipelines that use this function to consider the return
        // value of this function (that was not consider before)
        return ""
    }
    def data = readFile(file: versions_file_path)
    def jsonSlurper = new JsonSlurper()
    def json_data = jsonSlurper.parseText(data)
    println(func_name + "the JSON file parsed has the following content:" + json_data)
    dswp_version = json_data.DSWP
    st_version = json_data.ST
    dswp_ver_part_in_log_title_after_removal = dswp_version.replaceAll(" ","-")
    println(func_name + "extracted DSWP version:" + dswp_ver_part_in_log_title_after_removal + " and ST version:" + st_version)
    versions_part_in_command = ""
    if (!dswp_version.isEmpty() && !st_version.isEmpty())
    {
        versions_part_in_command = "DSWP:" + dswp_ver_part_in_log_title_after_removal + "__ST:" + st_version
    }
    def log_title_part_in_command = ""
    if (!versions_part_in_command.isEmpty())
    {
        log_title_part_in_command = " --logtitle " + versions_part_in_command
    }
    else
    {
        println("The versions part in log title is empty")
    }
    robot_command += log_title_part_in_command
    // add the VIL Memory Leak Scenario argument file
    String vil_memory_leak_scenario_argument_file_name = workspace_path + "/nptAutomationTesting/utilities/vilMemoryLeakScenarioUtility/vilMemoryLeakScenario.args"
    robot_command += " --argumentfile " + vil_memory_leak_scenario_argument_file_name
    return robot_command
}

def sendEmail(String subject, Boolean isRobotRun=true, String buildUrl="")
{
    def content = "Check console output at " + buildUrl + " to view the results."
    if (isRobotRun)
    {
        content = '${JELLY_SCRIPT,template="html"}'
    }
    // config email recipients
    def mailRecipients = [ [$class: 'RequesterRecipientProvider'], [$class: 'DevelopersRecipientProvider'], [$class: 'CulpritsRecipientProvider'] ]
    mailRecipients.unique() // remove duplicates
    mailRecipients.removeAll { !it } // remove null or false elements

    emailext(
        to: '$DEFAULT_RECIPIENTS',
        recipientProviders: mailRecipients,
        subject: subject,
        body: content,
        mimeType: 'text/html',
        replyTo: '$DEFAULT_REPLYTO',
        attachLog: false)
}

return this
