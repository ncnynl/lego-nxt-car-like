#!/usr/bin/env python

import roslib; roslib.load_manifest('nxt_carlike')  
import rospy
import math
import thread
from art_msgs.msg import CarDriveStamped
from nxt_lejos_ros_msgs.msg import JointPosition, JointVelocity
from math import sin, cos

global my_lock
my_lock = thread.allocate_lock()

class NXT_steering:
  def __init__(self):
        self.sub = rospy.Subscriber('pilot/drive', CarDriveStamped, self.sub_cb)
        self.pub_pos = rospy.Publisher('joint_position', JointPosition)
        self.pub_vel = rospy.Publisher('joint_velocity', JointVelocity)
	self.last_speed = 0;
	self.last_steering_angle= 0;
	self.ratioGearSteering = 5;

  def sub_cb(self, msg):
	if (msg.control.speed <> self.last_speed):
	  JC = JointVelocity();
	  JC.name = 'power_motor';
	  JC.velocity = -msg.control.speed*100; #convert m/s to deg/s for Lego NXT_steering
	  self.last_speed = msg.control.speed;
	  self.pub_vel.publish(JC)
	if (msg.control.steering_angle <> self.last_steering_angle):
	  JC = JointPosition();
	  JC.name = 'steering_motor';
	  JC.angle = msg.control.steering_angle*360/(2*math.pi)*self.ratioGearSteering;
	  self.last_steering_angle = msg.control.steering_angle;
	  self.pub_pos.publish(JC)	  
	  

def main():
    rospy.init_node('nxt_steering_control')
    nxt_steering = NXT_steering()
    rospy.spin()

if __name__ == '__main__':
    main()
