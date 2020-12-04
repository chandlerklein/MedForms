#!/bin/bash

#This script installs ssh. 
#adds necessary entries into /etc/ssh/sshd_config
#NOTE: the entries should be checked to confirm
#adds user group sftp
#adds user sftpuser
#allows for password setting
#if this script fails it is necessary to manually "clean up" before running again
	#sudo deluser --remove-home sftpuser

#make sure to run as root

apt install ssh -y

echo -e "Match group sftp\nChrootDirectory /home\nX11Forwarding no\nAllowTcpForwarding no\nForceCommand internal-sftp" >> /etc/ssh/sshd_config

service ssh restart

addgroup sftp

#exec useradd -m sftpuser -g sftp

if [ $(id -u) -eq 0 ]; then
	username="sftpuser"
	read -s -p "Enter password : " password
	egrep "^$username" /etc/passwd >/dev/null
	if [ $? -eq 0 ]; then
		echo "$username exists!"
		exit 1
	else
		pass=$(perl -e 'print crypt($ARGV[0], "password")' $password)
		useradd -m $username -g sftp -p $pass 
		[ $? -eq 0 ] && echo "User has been added to system!" || echo "Failed to add a user!"
	fi
else
	echo "Only root may add a user to the system"
	exit 2
fi

#chmod 700 /home/sftpuser/
