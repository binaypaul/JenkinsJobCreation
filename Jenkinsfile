pipeline {
  
  agent any
  
  stages {
  
    stage("build") {
      steps {
        echo 'building the app.. NAME:' ${NAME}
      }
    }
    
    stage("test") {
      steps {
        echo "testing the app.. ${Gender}"
      }
    }
    
    stage("deploy") {
      steps {
        echo 'deploying the app..'
      }
    }
  }
  
}
