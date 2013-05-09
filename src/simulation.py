def stop_condition():
    stop_condition.num_takers, stop_condition.total_takers = 100, 0
    stop_condition.total_takers = stop_condition.total_takers + 1
    return stop_condition.num_takers < stop_condition.total_takers

qs, agent_list =  [q1, q2, q3, q4], [CollegeStudent() for _ in range(100)]

survey = Survey(qs)
