def analyze_current_player_values(dataset, player=-1):
    positive = 0
    negative = 0
    zero = 0
    for entry in dataset:
        if entry['current_player'] == player:
            if entry['value'] > 0:
                positive += 1
            elif entry['value'] < 0:
                negative += 1
            else:
                zero += 1
    return {
        'positive': positive,
        'negative': negative,
        'zero': zero,
        'total': positive + negative + zero
    }
