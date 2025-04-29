ssh -i "myEc2InstanceKey.pem" ubuntu@ec2-3-138-117-96.us-east-2.compute.amazonaws.com

chmod +x start.sh

kill -9 <PID>


sudo yum install java-11-openjdk-devel
wget -O /etc/yum.repos.d/jenkins.repo https://pkg.jenkins.io/redhat/jenkins.repo
rpm --import https://pkg.jenkins.io/redhat/jenkins.io-2023.key
sudo yum install jenkins
sudo systemctl enable jenkins
sudo systemctl start jenkins


java  -Xms512m -Xmx1g -jar target/pdf-compressor-1.0-SNAPSHOT.jar --server.port=8081



pipeline {
    agent any

    stages {
        stage('Clone Code') {
            steps {
                git branch: 'main', url: 'https://github.com/Ap1297/pdf-compressor.git'
            }
        }

        stage('Build App') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Deploy App') {
            steps {
                sshagent(['ec2-ssh-key']) {
                    sh '''
                    # Copy jar to server
                    scp -o StrictHostKeyChecking=no target/*.jar ubuntu@localhost:/home/ubuntu/app.jar

                    # Login to server and restart app
                    ssh ubuntu@localhost << EOF
                        pkill -f app.jar || true
                        sleep 5
                        nohup java -jar /home/ubuntu/app.jar --server.port=8081 > /home/ubuntu/app.log 2>&1 &
                    EOF
                    '''
                }
            }
        }
    }
}
