#!/usr/bin/env python

import roslib; roslib.load_manifest('nxt_carlike')  
import rospy
import math
import thread
from std_msgs.msg import Int32
from sensor_msgs.msg import Imu
from math import *


class Converter:
  def __init__(self):
        self.sub = rospy.Subscriber('android/imu', Imu, self.imu_cb)
        self.pub = rospy.Publisher('compass', Int32)
        self.compassValue = 0;
        self.sumOfCompassValue = 0;
        self.numberOfValues = 0;
        self.maxNumberOfValues = 10;

  def imu_cb(self, msg):
	#self.sumOfCompassValue = self.sumOfCompassValue + msg.orientation.x;
	x = atan2 (2*(msg.orientation.w*msg.orientation.x+msg.orientation.y*msg.orientation.z), 1-2*(msg.orientation.x*msg.orientation.x+msg.orientation.y*msg.orientation.y))
	y = asin (2*(msg.orientation.w*msg.orientation.y-msg.orientation.x*msg.orientation.z))
	z = atan2(2*(msg.orientation.w*msg.orientation.z+msg.orientation.x*msg.orientation.y), 1-2*(msg.orientation.y*msg.orientation.y+msg.orientation.z*msg.orientation.z))
	if self.numberOfValues>self.maxNumberOfValues:
	  rospy.loginfo('x='+str(x)+' y='+str(y)+' z='+str(z))
	  self.numberOfValues=0
	else:
	  self.numberOfValues=self.numberOfValues+1
	  
	
def main():
    rospy.init_node('compass')
    rospy.loginfo('Init compass')
    converter = Converter()
    rospy.spin()


if __name__ == '__main__':
    main()
