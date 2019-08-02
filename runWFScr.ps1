#
# powershell script for transmitting
# command files into WinFIOL channels
# # require WinFIOL installed
#

$logfile = "powershell.log"

$winfiol = New-Object -ComObject Ericsson.WinFIOL.1
$winfiol.SetClientName("Power Shell Script")
$winfiol.Show()
$winfiol.KeepOpen()
$winfiol.ShowStatusLine("Let's transmit!", 3000)

$docpath = [environment]::getfolderpath("mydocuments")
$CSVfile = "$docpath\powerShell\channels.csv"

$colStats = Import-CSV $CSVfile

foreach ($wfChannel in $colStats)
	{
		[ref] $n_cnannel = [int] $wfChannel.channel
		$path = $wfChannel.path
		$Chanfile = "$docpath\$path"
		try{
			$winfiol.OpenChannelFile($Chanfile, 0, $n_cnannel)
		}
		finally{
			$channel = $winfiol.GetChannelPointer($n_cnannel.value)
		}
		$channel.TerminalConnect("", "")
		Do{
			$status = $channel.GetPortStatus()
		}
		Until($status -eq 2)
		$channel.OutputLog(1, $logfile)
		$channel.Transmit("allip.cmd", 1)
		Do{
			$status = $channel.GetBusyStatus()
			#echo $status
		}
		Until($status -eq 2)
		$channel.TerminalRelease()
		$channel.OutputLog(0, "")
	}


#$winfiol.Close()
